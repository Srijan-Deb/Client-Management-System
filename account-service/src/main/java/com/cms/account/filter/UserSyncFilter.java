package com.cms.account.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Idempotent user sync filter for account-service (cms_account schema).
 * See client-service UserSyncFilter for full documentation.
 */
@Slf4j
@Component
public class UserSyncFilter extends OncePerRequestFilter {

    private static final String UPSERT_SQL = """
            INSERT INTO users (keycloak_id, email, full_name, is_active)
            VALUES (?, ?, ?, TRUE)
            ON DUPLICATE KEY UPDATE
              last_login  = NOW(6),
              email       = VALUES(email),
              full_name   = VALUES(full_name)
            """;

    private final JdbcTemplate jdbc;

    public UserSyncFilter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            try {
                String keycloakId = jwt.getSubject();
                String email      = jwt.getClaimAsString("email");
                String fullName   = jwt.getClaimAsString("name");

                if (keycloakId != null && email != null) {
                    jdbc.update(UPSERT_SQL,
                            keycloakId,
                            email,
                            fullName != null ? fullName : email);
                    log.debug("UserSync[account]: upserted user keycloak_id={}", keycloakId);
                }
            } catch (Exception ex) {
                log.warn("UserSync[account] failed for {} â€” continuing: {}",
                        request.getRequestURI(), ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator");
    }
}
