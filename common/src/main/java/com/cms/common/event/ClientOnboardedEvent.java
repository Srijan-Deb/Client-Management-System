package com.cms.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event published to topic <b>client-onboarded</b> by Client Service
 * whenever a new client is successfully created and persisted.
 *
 * <p><b>ID type decision (Phase 0 â†’ Phase 2):</b>
 * All entity PKs are {@code BIGINT AUTO_INCREMENT} in MySQL (mapped to {@code Long} in JPA).
 * The ERD uses {@code Int} notation which maps to {@code Long} here for future-safety
 * (INT overflows at ~2B rows; BIGINT at ~9.2 quintillion).
 *
 * <p>Consumed by:
 * <ul>
 *   <li>Notification Service â€” sends welcome email</li>
 *   <li>Account Service â€” creates a default account record (Phase 3)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOnboardedEvent {

    /** Topic name constant â€” use this instead of hard-coding strings. */
    public static final String TOPIC = "client-onboarded";

    /**
     * DB primary key of the newly created client (BIGINT AUTO_INCREMENT).
     * Matches {@code clients.client_id} in the ERD.
     */
    @NotNull
    private Long clientId;

    /**
     * The account this client belongs to.
     * Matches {@code clients.account_id â†’ accounts.account_id}.
     */
    private Long accountId;

    /** Client's primary email address (unique constraint enforced in DB). */
    @NotBlank
    @Email
    private String email;

    /** First name from {@code clients.first_name}. */
    @NotBlank
    private String firstName;

    /** Last name from {@code clients.last_name}. */
    @NotBlank
    private String lastName;

    /** Tier from {@code clients.tier} (e.g. STANDARD, PREMIUM). */
    private String tier;

    /** UTC timestamp when the client record was persisted. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant onboardedAt;
}
