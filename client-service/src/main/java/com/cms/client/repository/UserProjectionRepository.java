package com.cms.client.repository;

import com.cms.client.domain.entity.UserProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Read-only repository for the {@code users} projection table.
 * All writes are done by {@code UserSyncFilter} via JDBC UPSERT.
 */
public interface UserProjectionRepository extends JpaRepository<UserProjection, Long> {

    Optional<UserProjection> findByKeycloakId(String keycloakId);
}
