package com.bankingproject.notification_service.listener;

import com.bankingproject.notification_service.model.Notification;
import com.bankingproject.notification_service.service.NotificationService;
import com.bankingproject.notification_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receiveMessage(String message) {

        log.info("Notification reçue : {}", message);

        Notification notification =
                Notification.builder()
                        .userId(1L)
                        .type("TRANSACTION")
                        .message(message)
                        .status("SUCCESS")
                        .build();

        notificationService.save(notification);
    }
}