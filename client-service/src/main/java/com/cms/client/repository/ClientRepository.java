package com.cms.client.repository;

import com.cms.client.domain.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    /** Email uniqueness check â€” used by the cache-aside duplicate guard. */
    boolean existsByEmail(String email);

    Optional<Client> findByEmail(String email);

    /**
     * Eagerly fetches contacts and addresses in a single JOIN query.
     * Used by GET /clients/{id} to avoid LazyInitializationException outside a TX.
     */
    @EntityGraph(attributePaths = {"contacts", "addresses"})
    @Query("SELECT c FROM Client c WHERE c.clientId = :id")
    Optional<Client> findWithDetailsById(@Param("id") Long id);

    /**
     * Case-insensitive full-text search across first name, last name, and email.
     * LIKE %term% is sufficient for Phase 2; MySQL FULLTEXT deferred to Phase 7.
     */
    @Query("""
            SELECT c FROM Client c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<Client> searchByTerm(@Param("term") String term, Pageable pageable);
}
