package com.cms.client.domain.entity;

import com.cms.client.domain.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA entity for the {@code addresses} table.
 * Each address belongs to exactly one {@link Client} and is cascade-deleted with it.
 */
@Entity
@Table(name = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "address_id")
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @ToString.Exclude
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false,
            columnDefinition = "ENUM('BILLING','SHIPPING','PRIMARY')")
    @Builder.Default
    private AddressType addressType = AddressType.PRIMARY;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "India";

    // Field name 'primary' â†’ Lombok generates isPrimary() getter, setPrimary() setter,
    // primary() builder method â†’ MapStruct property is uniformly "primary".
    // @Column(name = "is_primary") keeps the DB column name independent of Java convention.
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
