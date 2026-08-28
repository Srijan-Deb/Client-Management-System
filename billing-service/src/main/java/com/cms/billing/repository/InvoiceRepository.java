package com.cms.billing.repository;

import com.cms.billing.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByClientIdOrderByCreatedAtDesc(Long clientId);
    long countByStatus(String status);
}
