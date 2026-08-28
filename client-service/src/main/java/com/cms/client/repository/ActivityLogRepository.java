package com.cms.client.repository;

import com.cms.client.domain.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
