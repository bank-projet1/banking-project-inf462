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
    private final AfricasTalkingSmsService africasTalkingSmsService;

    public Notification save(Notification notification) {
        Notification saved = repository.save(notification);
        if (!"SMS".equalsIgnoreCase(saved.getType())) {
            return saved;
        }

        AfricasTalkingSmsService.SmsDeliveryResult result = africasTalkingSmsService.sendSms(saved.getPhoneNumber(), saved.getMessage());
        if (result.sent()) {
            saved.setStatus("ACCEPTED");
        } else if (result.skipped()) {
            saved.setStatus("SKIPPED");
        } else {
            saved.setStatus("FAILED");
        }
        return repository.save(saved);
    }

    public List<Notification> getAllNotifications() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<Notification> getNotificationsByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
