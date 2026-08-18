package com.cms.client.service;

import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.request.UpdateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.dto.response.ClientSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface ClientService {

    ClientResponse createClient(CreateClientRequest request, Jwt jwt);

    ClientResponse getClientById(Long id);

    Page<ClientSummaryResponse> searchClients(String search, Pageable pageable);

    ClientResponse updateClient(Long id, UpdateClientRequest request, Jwt jwt);
}
