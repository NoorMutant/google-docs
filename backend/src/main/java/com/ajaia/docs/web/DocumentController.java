package com.ajaia.docs.web;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.service.CurrentUserService;
import com.ajaia.docs.service.DocumentImportService;
import com.ajaia.docs.service.DocumentService;
import com.ajaia.docs.service.ImportResult;
import com.ajaia.docs.web.dto.CreateDocumentRequest;
import com.ajaia.docs.web.dto.DocumentDetail;
import com.ajaia.docs.web.dto.DocumentSummary;
import com.ajaia.docs.web.dto.UpdateDocumentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documents;
    private final DocumentImportService importer;
    private final CurrentUserService currentUser;

    public DocumentController(DocumentService documents,
                              DocumentImportService importer,
                              CurrentUserService currentUser) {
        this.documents = documents;
        this.importer = importer;
        this.currentUser = currentUser;
    }

    /**
     * The dashboard needs both lists at once, so they come back together
     * instead of as two round trips.
     */
    @GetMapping
    public Map<String, List<DocumentSummary>> list() {
        AppUser user = currentUser.require();
        return Map.of(
                "owned", documents.listOwned(user),
                "sharedWithMe", documents.listSharedWith(user));
    }

    @GetMapping("/{id}")
    public DocumentDetail get(@PathVariable Long id) {
        return documents.get(id, currentUser.require());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDetail create(@Valid @RequestBody(required = false) CreateDocumentRequest request) {
        String title = request == null ? null : request.title();
        return documents.create(title, "", currentUser.require());
    }

    @PatchMapping("/{id}")
    public DocumentDetail update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        return documents.update(id, request, currentUser.require());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documents.delete(id, currentUser.require());
    }

    /**
     * Uploading a .txt, .md or .docx file creates a new document owned by the
     * uploader. The size and type limits live in DocumentImportService.
     */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDetail importFile(@RequestParam("file") MultipartFile file) {
        ImportResult parsed = importer.parse(file);
        return documents.create(parsed.title(), parsed.html(), currentUser.require());
    }
}
