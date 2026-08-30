package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.AppUser;

public record UserSummary(Long id, String email, String displayName) {

    public static UserSummary of(AppUser user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
