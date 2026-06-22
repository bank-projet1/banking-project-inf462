package com.bankingproject.notification_service.controller;

import com.bankingproject.notification_service.model.Notification;
import com.bankingproject.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getNotificationsByUser(
            @PathVariable Long userId) {

        return notificationService.getNotificationsByUser(userId);
    }
}