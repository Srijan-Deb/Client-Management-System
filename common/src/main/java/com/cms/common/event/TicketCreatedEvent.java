package com.cms.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event published to topic <b>ticket-created</b> by the Support module
 * within Client Service whenever a new support ticket is opened.
 *
 * <p>All ID fields are {@code Long} matching BIGINT AUTO_INCREMENT PKs in MySQL.
 *
 * <p>Note: Support is implemented as a module inside Client Service for MVP
 * (Phase 6), not a separate microservice. If ticket volume grows, it can be
 * extracted into its own service without changing this event contract.
 *
 * <p>Consumed by:
 * <ul>
 *   <li>Notification Service â€” sends ticket confirmation email to the client</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreatedEvent {

    /** Topic name constant. */
    public static final String TOPIC = "ticket-created";

    /** {@code support_tickets.ticket_id} â€” BIGINT PK. */
    @NotNull
    private Long ticketId;

    /** {@code support_tickets.client_id} â€” FK to clients. */
    @NotNull
    private Long clientId;

    /**
     * Denormalised recipient email address â€” client's primary email at ticket creation time.
     * Carried on the event so Notification Service can send the confirmation email
     * without making a synchronous call to Client Service.
     */
    @NotBlank
    private String recipientEmail;

    /**
     * Account context for the ticket.
     * Matches {@code support_tickets.account_id} in the updated ERD.
     */
    private Long accountId;

    /**
     * Agent assigned to this ticket.
     * Matches {@code support_tickets.assigned_to â†’ users.user_id}.
     * Null if unassigned at creation time.
     */
    private Long assignedToUserId;

    /** Short summary of the issue (max 255 chars). Matches {@code support_tickets.subject}. */
    @NotBlank
    private String subject;

    /**
     * Ticket priority. Matches {@code support_tickets.priority}.
     * Allowed values: LOW, MEDIUM, HIGH, CRITICAL
     */
    @NotBlank
    private String priority;

    /**
     * Ticket category. Matches {@code support_tickets.category}.
     * (e.g. BILLING, TECHNICAL, ACCOUNT, GENERAL)
     */
    private String category;

    /**
     * Full description of the issue. Matches {@code support_tickets.description}.
     * Denormalised onto the event so Notification Service can render it in the
     * confirmation email without a synchronous call back to Client Service.
     */
    private String description;


    /** Email address of the assigned support agent (for notification routing). */
    private String assignedAgentEmail;

    /** UTC timestamp when the ticket was created. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
