package com.cms.client.dto.request;

import com.cms.client.domain.enums.ClientStatus;
import com.cms.client.domain.enums.ClientTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code PUT /api/v1/clients/{id}}.
 * All fields are optional â€” only non-null fields are applied (partial-update semantics).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    @Pattern(
            regexp = "^[^<>&\"']*$",
            message = "companyName must not contain HTML special characters (< > & \" ')")
    private String companyName;

    private ClientTier tier;

    private ClientStatus status;
}
