package com.example.transaction_service.client;

import com.example.transaction_service.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${external.service.notification-url:http://localhost:8087}")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    void createNotification(@RequestBody NotificationRequest notification);
}
