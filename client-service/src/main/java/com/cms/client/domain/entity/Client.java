package com.cms.client.domain.entity;

import com.cms.client.domain.enums.ClientStatus;
import com.cms.client.domain.enums.ClientTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code clients} table in {@code cms_client} schema.
 *
 * <p><b>account_id design:</b> Nullable at DB level to allow the two-insert ordering
 * pattern: client is INSERTed first (NULL account_id), Account Service is called
 * synchronously inside the same {@code @Transactional} boundary to get the account_id,
 * then the field is patched before commit. Callers never see a client without account_id.
 */
@Entity
@Table(name = "clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "client_id")
    private Long clientId;

    /** Linked after Account Service provisioning â€” never null when returned to callers. */
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "company_name")
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, columnDefinition = "ENUM('STANDARD','PREMIUM','ENTERPRISE')")
    @Builder.Default
    private ClientTier tier = ClientTier.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('ACTIVE','INACTIVE','SUSPENDED')")
    @Builder.Default
    private ClientStatus status = ClientStatus.ACTIVE;

    /**
     * FK to {@code users.user_id} â€” stored as a plain Long (not @ManyToOne) to avoid
     * cross-service JPA joins. The users table is a local projection populated by
     * UserSyncFilter on every authenticated request.
     */
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Contact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    // â”€â”€ Helper methods â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void addContact(Contact contact) {
        contact.setClient(this);
        contacts.add(contact);
    }

    public void addAddress(Address address) {
        address.setClient(this);
        addresses.add(address);
    }
}
