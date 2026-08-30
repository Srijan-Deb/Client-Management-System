package com.cms.client.service;

import com.cms.client.domain.entity.SupportTicket;
import com.cms.client.dto.request.TicketRequest;
import com.cms.client.dto.response.TicketResponse;

import com.cms.client.domain.entity.Client;
import com.cms.client.domain.entity.UserProjection;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.SupportTicketRepository;
import com.cms.client.repository.TicketCommentRepository;
import com.cms.client.repository.UserProjectionRepository;
import com.cms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SupportTicketService â€” Unit Tests")
class SupportTicketServiceTest {

    @Mock private SupportTicketRepository ticketRepository;
    @Mock private TicketCommentRepository commentRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserProjectionRepository userProjectionRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SupportTicketService ticketService;

    private Jwt mockJwt;
    private SupportTicket mockTicket;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject()).thenReturn("kc-uuid-123");
        when(mockJwt.getClaimAsString("preferred_username")).thenReturn("testuser");

        Client mockClient = Client.builder().clientId(1L).build();
        UserProjection mockUser = mock(UserProjection.class);
        when(mockUser.getUserId()).thenReturn(1L);
        when(mockUser.getKeycloakId()).thenReturn("kc-uuid-123");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));
        when(userProjectionRepository.findByKeycloakId("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(userProjectionRepository.findByKeycloakId("kc-uuid-123")).thenReturn(Optional.of(mockUser));

        mockTicket = SupportTicket.builder()
                .ticketId(10L)
                .clientId(1L)
                .accountId(100L)
                .subject("Test Issue")
                .description("Cannot login")
                .category("AUTH")
                .priority("HIGH")
                .status("OPEN")
                .build();
    }

    @Test
    @DisplayName("createTicket: persists ticket, writes log, sends Kafka event")
    void createTicket_success() {
        TicketRequest request = new TicketRequest();
        request.setClientId(1L);
        request.setSubject("Test Issue");
        request.setDescription("Cannot login");
        request.setCategory("AUTH");
        request.setPriority("HIGH");

        when(ticketRepository.save(any())).thenReturn(mockTicket);

        TicketResponse result = ticketService.createTicket(request, mockJwt);

        assertThat(result.getTicketId()).isEqualTo(10L);
        verify(ticketRepository).save(any());
        verify(activityLogRepository).save(any());
        verify(kafkaTemplate).send(eq("ticket-created"), any());
    }

    @Test
    @DisplayName("getTicketById: returns mapped response")
    void getTicketById_success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));

        TicketResponse result = ticketService.getTicketById(10L);

        assertThat(result.getTicketId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getTicketById: throws 404 when not found")
    void getTicketById_notFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("closeTicket: sets status to CLOSED and writes log")
    void closeTicket_success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        
        SupportTicket closedTicket = SupportTicket.builder()
                .ticketId(10L)
                .status("CLOSED")
                .build();
                
        when(ticketRepository.save(any())).thenReturn(closedTicket);

        TicketResponse result = ticketService.closeTicket(10L, mockJwt);

        assertThat(result.getStatus()).isEqualTo("CLOSED");
        verify(activityLogRepository).save(argThat(log -> log.getAction().equals("TICKET_CLOSED")));
    }
}
