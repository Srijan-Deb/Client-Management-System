package com.cms.notification.repository;

import com.cms.notification.domain.entity.Notification;
import com.cms.notification.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByRecipientEmail(String email);
}
