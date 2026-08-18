package com.cms.client.repository;

import com.cms.client.domain.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketTicketIdOrderByCreatedAtAsc(Long ticketId);
}
