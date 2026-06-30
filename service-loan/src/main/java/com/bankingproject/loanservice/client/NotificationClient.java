package com.bankingproject.loanservice.client;

import com.bankingproject.loanservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public NotificationClient(RestTemplate restTemplate,
                              @Value("${external.service.notification-url:http://localhost:8088}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public void sendLoanNotification(LoanNotificationRequest request) {
        try {
            restTemplate.postForEntity(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach notification-service at " + notificationServiceUrl, ex);
        }
    }

    public void sendNotification(NotificationRequest request) {
        try {
            restTemplate.postForEntity(notificationServiceUrl + "/api/notifications", request, Void.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach notification-service at " + notificationServiceUrl, ex);
        }
    }
}
