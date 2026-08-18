package com.cms.client.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA entity for the {@code activity_logs} audit table.
 *
 * <p>Both {@code clientId} and {@code userId} are stored as plain {@code Long}s
 * (not {@code @ManyToOne}) because:
 * <ul>
 *   <li>Both FKs use {@code ON DELETE SET NULL} â€” JPA would null the field anyway.</li>
 *   <li>Avoids eager/lazy loading overhead on every log write.</li>
 * </ul>
 */
@Entity
@Table(name = "activity_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    @Builder.Default
    private String entityType = "CLIENT";

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
