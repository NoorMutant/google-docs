package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.DocumentShare;
import com.ajaia.docs.domain.ShareRole;

public record ShareView(Long userId, String email, String displayName, ShareRole role) {

    public static ShareView of(DocumentShare share) {
        return new ShareView(
                share.getUser().getId(),
                share.getUser().getEmail(),
                share.getUser().getDisplayName(),
                share.getRole());
    }
}
