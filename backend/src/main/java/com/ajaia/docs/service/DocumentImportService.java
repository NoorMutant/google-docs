package com.ajaia.docs.service;

import com.ajaia.docs.web.BadRequestException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Turns an uploaded file into the title and HTML body of a new document.
 * Supported types are listed in SUPPORTED_EXTENSIONS and repeated in the UI.
 */
@Service
public class DocumentImportService {

    public static final long MAX_IMPORT_BYTES = 2L * 1024 * 1024;
    public static final List<String> SUPPORTED_EXTENSIONS = List.of("txt", "md", "markdown", "docx");

    private final HtmlSanitizer sanitizer;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder().build();

    public DocumentImportService(HtmlSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    public ImportResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Pick a file to import");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new BadRequestException("Imported files are limited to 2 MB");
        }

        String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "imported";
        String extension = extensionOf(filename);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "Only .txt, .md and .docx files can be imported. Received ." + extension);
        }

        String html = switch (extension) {
            case "md", "markdown" -> markdownToHtml(readText(file));
            case "docx" -> docxToHtml(file);
            default -> plainTextToHtml(readText(file));
        };

        return new ImportResult(titleFrom(filename), sanitizer.clean(html));
    }

    private String markdownToHtml(String markdown) {
        return markdownRenderer.render(markdownParser.parse(markdown));
    }

    private String plainTextToHtml(String text) {
        StringBuilder html = new StringBuilder();
        // A blank line starts a new paragraph, a single newline is a line break.
        for (String block : text.replace("\r\n", "\n").split("\n[ \t]*\n")) {
            if (block.isBlank()) {
                continue;
            }
            html.append("<p>").append(escape(block.trim()).replace("\n", "<br>")).append("</p>");
        }
        return html.isEmpty() ? "<p></p>" : html.toString();
    }

    private String docxToHtml(MultipartFile file) {
        try (InputStream in = file.getInputStream(); XWPFDocument docx = new XWPFDocument(in)) {
            StringBuilder html = new StringBuilder();
            boolean inList = false;

            for (XWPFParagraph paragraph : docx.getParagraphs()) {
                String body = runsToHtml(paragraph);
                if (body.isBlank()) {
                    continue;
                }

                boolean isListItem = paragraph.getNumID() != null;
                if (isListItem && !inList) {
                    html.append("<ul>");
                    inList = true;
                } else if (!isListItem && inList) {
                    html.append("</ul>");
                    inList = false;
                }

                if (isListItem) {
                    html.append("<li>").append(body).append("</li>");
                } else {
                    html.append("<").append(tagFor(paragraph)).append(">")
                            .append(body)
                            .append("</").append(tagFor(paragraph)).append(">");
                }
            }
            if (inList) {
                html.append("</ul>");
            }
            return html.isEmpty() ? "<p></p>" : html.toString();
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("That .docx file could not be read. It may be corrupt or password protected");
        }
    }

    /**
     * Word heading styles are named Heading1, Heading2 and so on. Anything else
     * becomes a normal paragraph.
     */
    private String tagFor(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        if (style == null) {
            return "p";
        }
        return switch (style.toLowerCase(Locale.ROOT).replace(" ", "")) {
            case "heading1", "title" -> "h1";
            case "heading2" -> "h2";
            case "heading3" -> "h3";
            default -> "p";
        };
    }

    private String runsToHtml(XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String value = run.text();
            if (value == null || value.isEmpty()) {
                continue;
            }
            String escaped = escape(value);
            if (run.isBold()) {
                escaped = "<strong>" + escaped + "</strong>";
            }
            if (run.isItalic()) {
                escaped = "<em>" + escaped + "</em>";
            }
            text.append(escaped);
        }
        return text.toString();
    }

    private String readText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read");
        }
    }

    private String titleFrom(String filename) {
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String withoutExtension = dot > 0 ? name.substring(0, dot) : name;
        return withoutExtension.isBlank() ? "Imported document" : withoutExtension.trim();
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
