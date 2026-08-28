package com.cms.client.service;

import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.request.UpdateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.dto.response.ClientSummaryResponse;
import com.cms.client.dto.response.ActivityLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface ClientService {

    ClientResponse createClient(CreateClientRequest request, Jwt jwt);

    ClientResponse getClientById(Long id);

    Page<ClientSummaryResponse> searchClients(String search, Pageable pageable);

    ClientResponse updateClient(Long id, UpdateClientRequest request, Jwt jwt);

    List<ActivityLogResponse> getClientActivityLogs(Long id);
}
