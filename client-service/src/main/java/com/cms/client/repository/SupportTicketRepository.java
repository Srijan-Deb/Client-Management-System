package com.cms.client.repository;

import com.cms.client.domain.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findByClientId(Long clientId, Pageable pageable);
    List<SupportTicket> findByAssignedTo(Long userId);
    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.comments WHERE t.ticketId = :ticketId")
    java.util.Optional<SupportTicket> findWithCommentsById(@org.springframework.data.repository.query.Param("ticketId") Long ticketId);
}
