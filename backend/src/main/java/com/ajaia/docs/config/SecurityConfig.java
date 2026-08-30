package com.ajaia.docs.config;

import com.ajaia.docs.repo.AppUserRepository;
import com.ajaia.docs.web.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; "
                    + "script-src 'self'; "
                    // Angular writes component styles into inline style tags.
                    + "style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; "
                    + "font-src 'self'; "
                    + "connect-src 'self'; "
                    + "object-src 'none'; "
                    + "frame-ancestors 'none'; "
                    + "base-uri 'self'; "
                    + "form-action 'self'";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities("ROLE_USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        // The Angular client reads the XSRF-TOKEN cookie and echoes it back in
        // the X-XSRF-TOKEN header, so the token repository has to be readable
        // from JavaScript and the plain request handler has to be used.
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                // Written as separate statements rather than a chain because
                // these builder methods do not all return the same type.
                .headers(headers -> {
                    // The app stores user authored HTML, so a content security
                    // policy is the backstop if anything ever slips past the
                    // sanitizer. No inline script, nothing loaded off site.
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY));
                    headers.referrerPolicy(referrer ->
                            referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicy(permissions ->
                            permissions.policy("camera=(), microphone=(), geolocation=(), payment=()"));
                    // Browsers only honour this over HTTPS, so it is inert in
                    // local development and active on the deployed instance.
                    headers.httpStrictTransportSecurity(hsts -> {
                        hsts.includeSubDomains(true);
                        hsts.maxAgeInSeconds(31536000);
                    });
                })
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/auth/login", "/api/auth/demo-users").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is the compiled Angular app and its assets.
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, "Please sign in"))
                        // Mostly a missing or stale CSRF token. Reloading the page
                        // gets a fresh one.
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, objectMapper, HttpStatus.FORBIDDEN,
                                        "Your session token expired. Reload the page and try again")))
                .logout(logout -> logout.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    private void writeError(HttpServletResponse response, ObjectMapper mapper,
                            HttpStatus status, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), new ApiError(status.value(), message));
    }
}
