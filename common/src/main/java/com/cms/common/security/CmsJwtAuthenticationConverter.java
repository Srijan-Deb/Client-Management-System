package com.cms.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Custom JWT â†’ Spring Authentication converter for Keycloak JWTs.
 *
 * <p>Keycloak encodes realm roles in the JWT as:
 * <pre>
 * {
 *   "realm_access": {
 *     "roles": ["admin", "support_agent", "offline_access", ...]
 *   }
 * }
 * </pre>
 *
 * <p>The default Spring Security converter only reads {@code scope} claims,
 * so this converter is required to extract Keycloak realm roles and map them
 * to {@link GrantedAuthority} objects with the {@code ROLE_} prefix that
 * {@code @PreAuthorize("hasRole('admin')")} expects.
 *
 * <p>Keycloak-internal roles ({@code offline_access}, {@code uma_authorization},
 * {@code default-roles-*}) are filtered out to keep the authority set clean.
 *
 * <h3>Usage in SecurityConfig:</h3>
 * <pre>{@code
 * @Bean
 * public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *     return http
 *         .oauth2ResourceServer(oauth2 -> oauth2
 *             .jwt(jwt -> jwt.jwtAuthenticationConverter(new CmsJwtAuthenticationConverter())))
 *         .build();
 * }
 * }</pre>
 */
public class CmsJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Default Spring converter â€” extracts 'scope'/'scp' claims as SCOPE_* authorities. */
    private final JwtGrantedAuthoritiesConverter defaultConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // 1. Collect default scope-based authorities (e.g., SCOPE_openid)
        Collection<GrantedAuthority> authorities = new ArrayList<>(
                defaultConverter.convert(jwt));

        // 2. Extract realm_access.roles from Keycloak JWT
        authorities.addAll(extractRealmRoles(jwt));

        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extracts {@code realm_access.roles} from the JWT and maps each non-internal
     * role to a {@code ROLE_<name>} {@link SimpleGrantedAuthority}.
     */
    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess =
                jwt.getClaimAsMap(SecurityConstants.CLAIM_REALM_ACCESS);

        if (realmAccess == null || !realmAccess.containsKey(SecurityConstants.CLAIM_ROLES)) {
            return List.of();
        }

        List<String> roles = (List<String>) realmAccess.get(SecurityConstants.CLAIM_ROLES);

        return roles.stream()
                .filter(role -> !SecurityConstants.KC_INTERNAL_ROLES.contains(role))
                .map(role -> new SimpleGrantedAuthority(
                        SecurityConstants.ROLE_PREFIX + role))
                .toList();
    }
}
