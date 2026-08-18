package com.cms.client.dto.response;

import com.cms.client.domain.enums.ClientStatus;
import com.cms.client.domain.enums.ClientTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Full client response â€” returned by POST /clients and GET /clients/{id}.
 * Implements {@link Serializable} for Redis serialization (Jackson handles the
 * actual bytes; Serializable provides a fallback safety net).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long clientId;
    private Long accountId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String companyName;
    private ClientTier tier;
    private ClientStatus status;
    private List<ContactResponse> contacts;
    private List<AddressResponse> addresses;
    private Instant createdAt;
    private Instant updatedAt;
}
