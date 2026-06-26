package com.bankingproject.notification_service.repository;

import com.bankingproject.notification_service.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
