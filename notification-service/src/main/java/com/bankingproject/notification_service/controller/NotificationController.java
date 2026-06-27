package com.bankingproject.notification_service.controller;

import com.bankingproject.notification_service.model.Notification;
import com.bankingproject.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/notifications", "/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @PostMapping
    public Notification createNotification(@RequestBody Notification notification) {
        return notificationService.save(notification);
    }

    @PostMapping("/test-sms")
    public Notification testSms(@RequestParam String phoneNumber) {
        Notification notification = Notification.builder()
                .type("SMS")
                .message("Test SMS Banking Project: votre service de notification fonctionne.")
                .status("PENDING")
                .phoneNumber(phoneNumber)
                .build();
        return notificationService.save(notification);
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getNotificationsByUser(
            @PathVariable Long userId) {

        return notificationService.getNotificationsByUser(userId);
    }
}
