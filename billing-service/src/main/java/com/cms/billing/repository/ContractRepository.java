package com.cms.billing.repository;

import com.cms.billing.domain.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    /** Find all contracts for a given client, newest first. */
    List<Contract> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
