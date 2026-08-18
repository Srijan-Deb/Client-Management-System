package com.cms.client.dto.response;

import com.cms.client.domain.enums.ContactType;
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
public class ContactResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long contactId;
    private ContactType contactType;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Instant createdAt;
}
