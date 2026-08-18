package com.cms.client.dto.request;

import com.cms.client.domain.enums.ClientTier;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/v1/clients}.
 * All validation annotations map to HTTP 400 responses via GlobalExceptionHandler.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Pattern(
            regexp = "^[+]?[\\d\\s\\-().]{7,20}$",
            message = "Phone number format is invalid"
    )
    private String phone;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    @Pattern(
            regexp = "^[^<>&\"']*$",
            message = "companyName must not contain HTML special characters (< > & \" ')")
    private String companyName;

    @NotNull(message = "Tier is required")
    private ClientTier tier;
}
