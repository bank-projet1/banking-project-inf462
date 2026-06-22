package com.bankingproject.notification_service.service;

import com.bankingproject.notification_service.model.Notification;
import com.bankingproject.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return repository.findAll();
    }

    public List<Notification> getNotificationsByUser(Long userId) {
        return repository.findByUserId(userId);
    }
}