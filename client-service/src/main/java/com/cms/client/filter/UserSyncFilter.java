package com.cms.client.filter;

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
 * Idempotent user sync filter for client-service.
 *
 * <p>On every authenticated request, upserts the caller's identity into
 * the local {@code cms_client.users} projection table. This ensures FK
 * constraints on {@code activity_logs.user_id}, {@code support_tickets.assigned_to},
 * and {@code ticket_comments.user_id} are always satisfiable from Phase 2 onward,
 * without depending on Account Service being built first.
 *
 * <p>Uses {@code INSERT ... ON DUPLICATE KEY UPDATE} on the {@code keycloak_id}
 * unique key â€” safe to call on every request with no race conditions.
 *
 * <p>Sync failures are logged at WARN but never propagate to the caller â€”
 * a sync error should not fail a legitimate business request.
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
                String keycloakId = jwt.getSubject();                          // sub claim
                String email      = jwt.getClaimAsString("email");
                String fullName   = jwt.getClaimAsString("name");

                if (keycloakId != null && email != null) {
                    jdbc.update(UPSERT_SQL,
                            keycloakId,
                            email,
                            fullName != null ? fullName : email);
                    log.debug("UserSync: upserted user keycloak_id={}", keycloakId);
                }
            } catch (Exception ex) {
                // Never fail the business request due to a sync error
                log.warn("UserSync failed for request {} â€” continuing: {}",
                        request.getRequestURI(), ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    /** Skip sync for non-authenticated paths (actuator, public endpoints). */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator");
    }
}
