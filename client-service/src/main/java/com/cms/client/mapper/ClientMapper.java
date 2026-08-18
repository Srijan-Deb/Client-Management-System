package com.cms.client.mapper;

import com.cms.client.domain.entity.Address;
import com.cms.client.domain.entity.Client;
import com.cms.client.domain.entity.Contact;
import com.cms.client.dto.request.AddressRequest;
import com.cms.client.dto.request.ContactRequest;
import com.cms.client.dto.response.AddressResponse;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.dto.response.ClientSummaryResponse;
import com.cms.client.dto.response.ContactResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper â€” generates implementation at compile time with zero runtime overhead.
 * Component model "spring" makes this a Spring-managed bean.
 */
@Mapper(componentModel = "spring")
public interface ClientMapper {

    /** Full response including embedded contacts and addresses. */
    ClientResponse toResponse(Client client);

    /** Lightweight summary for paginated list responses. */
    ClientSummaryResponse toSummaryResponse(Client client);

    ContactResponse toContactResponse(Contact contact);

    // Address.primary â†’ getter isPrimary() â†’ MapStruct property "primary"
    // AddressResponse.primary â†’ setter setPrimary() â†’ MapStruct property "primary"
    // Auto-mapped: no @Mapping needed.
    AddressResponse toAddressResponse(Address address);

    /** Maps ContactRequest â†’ Contact entity; client back-reference set by caller. */
    @Mapping(target = "contactId", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Contact toContactEntity(ContactRequest request);

    /** Maps AddressRequest â†’ Address entity; client back-reference set by caller. */
    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    // AddressRequest.primary (Boolean) â†’ getPrimary() â†’ MapStruct property "primary"
    // Address.primary (boolean) â†’ setPrimary() â†’ MapStruct property "primary" â†’ auto-mapped
    Address toAddressEntity(AddressRequest request);
}
