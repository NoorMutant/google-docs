package com.ajaia.docs.web;

import com.ajaia.docs.repo.AppUserRepository;
import com.ajaia.docs.service.CurrentUserService;
import com.ajaia.docs.service.LoginAttemptService;
import com.ajaia.docs.web.dto.LoginRequest;
import com.ajaia.docs.web.dto.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUser;
    private final AppUserRepository users;
    private final LoginAttemptService loginAttempts;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager,
                          CurrentUserService currentUser,
                          AppUserRepository users,
                          LoginAttemptService loginAttempts) {
        this.authenticationManager = authenticationManager;
        this.currentUser = currentUser;
        this.users = users;
        this.loginAttempts = loginAttempts;
    }

    @PostMapping("/login")
    public UserSummary login(@Valid @RequestBody LoginRequest request,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        String email = request.email().trim();

        if (loginAttempts.isLocked(email)) {
            long minutes = Math.max(1, loginAttempts.remainingLock(email).toMinutes());
            throw new TooManyRequestsException(
                    "Too many failed sign in attempts. Try again in " + minutes + " minutes");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            loginAttempts.recordFailure(email);
            // Same message either way so the response does not confirm which
            // email addresses exist.
            throw new UnauthorizedException("Email or password is not correct");
        }

        loginAttempts.recordSuccess(email);

        // Rotate the session id on login, then store the context so the next
        // request is recognised.
        HttpSession existing = httpRequest.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        httpRequest.getSession(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);

        return UserSummary.of(currentUser.require());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserSummary me() {
        return UserSummary.of(currentUser.require());
    }

    /**
     * Demo affordance. The login screen lists the seeded accounts so a reviewer
     * does not have to guess email addresses. A real product would not expose
     * this, and it would disappear along with the seeded users.
     */
    @GetMapping("/demo-users")
    public List<UserSummary> demoUsers() {
        return users.findAllByOrderByDisplayNameAsc().stream()
                .map(UserSummary::of)
                .toList();
    }
}
