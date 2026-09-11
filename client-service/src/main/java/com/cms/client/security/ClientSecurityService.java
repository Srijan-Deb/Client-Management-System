package com.cms.client.security;

import com.cms.client.domain.entity.Client;
import com.cms.client.domain.entity.SupportTicket;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("clientSecurityService")
@RequiredArgsConstructor
@Slf4j
public class ClientSecurityService {

    private final ClientRepository clientRepository;
    private final SupportTicketRepository supportTicketRepository;

    /**
     * Verifies if the authenticated JWT (email) is the owner of the requested clientId.
     */
    public boolean isOwner(Jwt jwt, Long targetClientId) {
        if (targetClientId == null) {
            return false;
        }
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            return false;
        }

        Optional<Client> clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isEmpty()) {
            return false;
        }

        return clientOpt.get().getClientId().equals(targetClientId);
    }

    /**
     * Verifies if the authenticated JWT (email) owns the support ticket.
     */
    public boolean isTicketOwner(Jwt jwt, Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        Optional<SupportTicket> ticketOpt = supportTicketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            return false;
        }
        return isOwner(jwt, ticketOpt.get().getClientId());
    }

    /**
     * Resolves the clientId for the given JWT.
     * Throws an exception if the client cannot be found (useful for service layer).
     */
    public Long resolveClientId(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new IllegalArgumentException("No email found in JWT");
        }
        return clientRepository.findByEmail(email)
                .map(Client::getClientId)
                .orElseThrow(() -> new IllegalArgumentException("No client found for email: " + email));
    }
}
