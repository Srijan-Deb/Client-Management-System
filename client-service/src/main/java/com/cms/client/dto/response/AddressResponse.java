package com.cms.client.dto.response;

import com.cms.client.domain.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long addressId;
    private AddressType addressType;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    // Use Boolean wrapper so Lombok generates getPrimary() â€” MapStruct property = "primary"
    // Primitive 'boolean primary' would generate isPrimary() â€” property = "isPrimary"
    private Boolean primary;
    private Instant createdAt;
}
