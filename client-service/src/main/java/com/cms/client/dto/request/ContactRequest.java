package com.cms.client.dto.request;

import com.cms.client.domain.enums.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded contact sub-object inside {@link CreateClientRequest}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {

    @NotNull(message = "Contact type is required")
    private ContactType contactType;

    @NotBlank(message = "Contact first name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Contact last name is required")
    @Size(max = 100)
    private String lastName;

    @Email(message = "Contact email must be a valid address")
    private String email;

    @Size(max = 20)
    private String phone;
}
