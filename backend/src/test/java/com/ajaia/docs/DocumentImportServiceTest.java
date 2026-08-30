package com.ajaia.docs;

import com.ajaia.docs.service.DocumentImportService;
import com.ajaia.docs.service.HtmlSanitizer;
import com.ajaia.docs.service.ImportResult;
import com.ajaia.docs.web.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentImportServiceTest {

    private final DocumentImportService service = new DocumentImportService(new HtmlSanitizer());

    @Test
    void markdownKeepsHeadingsAndLists() {
        String markdown = "# Release notes\n\nShipped **sharing** today.\n\n- Roles\n- Attachments\n";
        ImportResult result = service.parse(file("release-notes.md", markdown));

        assertThat(result.title()).isEqualTo("release-notes");
        assertThat(result.html())
                .contains("<h1>Release notes</h1>")
                .contains("<strong>sharing</strong>")
                .contains("<li>Roles</li>");
    }

    @Test
    void plainTextBecomesParagraphs() {
        ImportResult result = service.parse(file("notes.txt", "First block\n\nSecond block"));

        assertThat(result.html()).isEqualToIgnoringWhitespace("<p>First block</p><p>Second block</p>");
    }

    @Test
    void plainTextIsEscapedRatherThanTreatedAsMarkup() {
        ImportResult result = service.parse(file("notes.txt", "<script>alert(1)</script>"));

        assertThat(result.html()).doesNotContain("<script>");
    }

    @Test
    void unsupportedTypesAreRejectedWithAReadableMessage() {
        assertThatThrownBy(() -> service.parse(file("photo.png", "not really a png")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(".txt, .md and .docx");
    }

    @Test
    void emptyUploadIsRejected() {
        assertThatThrownBy(() -> service.parse(file("empty.txt", "")))
                .isInstanceOf(BadRequestException.class);
    }

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }
}
