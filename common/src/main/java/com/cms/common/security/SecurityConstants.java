package com.cms.common.security;

/**
 * Centralized role name constants - matches Keycloak realm role names
 * and the {@code roles.role_name} column in CMS_Schema_Merged.sql.
 *
 * <p>Usage in {@code @PreAuthorize}:
 * <pre>
 *   @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
 *   // or via SpEL shorthand:
 *   @PreAuthorize("hasRole('admin')")
 * </pre>
 *
 * <p>Spring Security prefixes roles with {@code ROLE_} internally when using
 * {@code hasRole()} - these constants store the unprefixed name as it appears
 * in the Keycloak JWT {@code realm_access.roles} array.
 */
public final class SecurityConstants {

    // --- Realm roles (match Keycloak role names exactly) ---
    public static final String ROLE_ADMIN           = "admin";
    public static final String ROLE_ACCOUNT_MANAGER = "account_manager";
    public static final String ROLE_SUPPORT_AGENT   = "support_agent";

    // --- JWT claim paths ---
    /** Top-level claim in Keycloak JWT containing realm-level role assignments. */
    public static final String CLAIM_REALM_ACCESS = "realm_access";

    /** Nested claim within {@link #CLAIM_REALM_ACCESS} containing the role name array. */
    public static final String CLAIM_ROLES = "roles";

    // --- Keycloak internal roles to exclude from Spring authorities ---
    /** Internal Keycloak roles that should NOT be mapped to Spring GrantedAuthorities. */
    public static final java.util.Set<String> KC_INTERNAL_ROLES = java.util.Set.of(
            "offline_access",
            "uma_authorization",
            "default-roles-cms",
            "default-roles-master"
    );

    // --- Spring Security authority prefix ---
    /** Prefix Spring Security applies to role names when using hasRole(). */
    public static final String ROLE_PREFIX = "ROLE_";

    private SecurityConstants() {
        // utility class - no instantiation
    }
}
