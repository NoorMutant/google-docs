package com.ajaia.docs.service;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.repo.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * The signed in user. Every endpoint behind the security filter has one,
     * so a missing user here means the filter chain is misconfigured.
     */
    public AppUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user on the request");
        }
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Signed in user is missing from the database"));
    }
}
