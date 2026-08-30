package com.ajaia.docs.web;

import com.ajaia.docs.service.CurrentUserService;
import com.ajaia.docs.service.ShareService;
import com.ajaia.docs.web.dto.ShareRequest;
import com.ajaia.docs.web.dto.ShareView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/shares")
public class ShareController {

    private final ShareService shares;
    private final CurrentUserService currentUser;

    public ShareController(ShareService shares, CurrentUserService currentUser) {
        this.shares = shares;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ShareView> list(@PathVariable Long documentId) {
        return shares.list(documentId, currentUser.require());
    }

    @PostMapping
    public ShareView share(@PathVariable Long documentId, @Valid @RequestBody ShareRequest request) {
        return shares.share(documentId, request.email(), request.role(), currentUser.require());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unshare(@PathVariable Long documentId, @PathVariable Long userId) {
        shares.unshare(documentId, userId, currentUser.require());
    }
}
