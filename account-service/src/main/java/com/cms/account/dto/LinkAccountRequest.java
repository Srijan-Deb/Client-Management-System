package com.cms.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/v1/accounts/link/{clientId}}.
 *
 * <p>Sent internally by Client Service during client onboarding.
 * {@code clientId} is a path variable â€” the body only carries the
 * human-readable fields needed to populate the account record.
 *
 * <p>No {@code clientId} field here: accounts do not store which client
 * triggered provisioning. The relationship lives on {@code clients.account_id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkAccountRequest {

    @NotBlank(message = "firstName is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid address")
    @Size(max = 255)
    private String email;
}
