package com.cms.client.repository;

import com.cms.client.domain.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByClientClientId(Long clientId);

    boolean existsByContactIdAndClientClientId(Long contactId, Long clientId);
}
