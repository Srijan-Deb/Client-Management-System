package com.cms.client.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Read-only JPA projection of the {@code users} table, which is populated and
 * maintained by {@code UserSyncFilter} on every authenticated request.
 *
 * <p>This entity intentionally exposes no setters / mutation methods â€” all writes
 * go through {@code UserSyncFilter} via JDBC UPSERT. This class is only used
 * to look up the local {@code user_id} from a Keycloak subject UUID.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class UserProjection {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "created_at")
    private Instant createdAt;
}
