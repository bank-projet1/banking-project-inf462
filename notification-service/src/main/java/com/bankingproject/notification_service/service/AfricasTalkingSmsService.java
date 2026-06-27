package com.bankingproject.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AfricasTalkingSmsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String username;
    private final String apiKey;
    private final String from;
    private final String smsUrl;
    private final boolean enabled;

    public AfricasTalkingSmsService(
            @Value("${africastalking.username:sandbox}") String username,
            @Value("${africastalking.api-key:}") String apiKey,
            @Value("${africastalking.from:}") String from,
            @Value("${africastalking.sms-url:https://api.sandbox.africastalking.com/version1/messaging}") String smsUrl,
            @Value("${africastalking.enabled:true}") boolean enabled) {
        this.username = username;
        this.apiKey = apiKey;
        this.from = from;
        this.smsUrl = smsUrl;
        this.enabled = enabled;
    }

    public SmsDeliveryResult sendSms(String toNumber, String message) {
        String normalizedNumber = normalizePhoneNumber(toNumber);
        if (!enabled) {
            return SmsDeliveryResult.skippedResult("Africa's Talking SMS is disabled. Set AFRICASTALKING_ENABLED=true to send real SMS.");
        }
        if (!isConfigured()) {
            return SmsDeliveryResult.failedResult("Africa's Talking credentials are missing. Set AFRICASTALKING_USERNAME and AFRICASTALKING_API_KEY.");
        }
        if (isBlank(normalizedNumber)) {
            return SmsDeliveryResult.failedResult("Recipient phone number is missing or invalid.");
        }
        if (isBlank(message)) {
            return SmsDeliveryResult.failedResult("SMS message is missing.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("apiKey", apiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("to", normalizedNumber);
        body.add("message", message);
        if (!isBlank(from)) {
            body.add("from", from);
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(smsUrl, new HttpEntity<>(body, headers), String.class);
            SmsDeliveryResult result = parseDeliveryResult(response.getBody(), normalizedNumber);
            if (result.sent()) {
                log.info("Africa's Talking SMS accepted for {}", normalizedNumber);
            } else {
                log.warn("Africa's Talking SMS not accepted for {}: {}", normalizedNumber, result.errorMessage());
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Africa's Talking SMS could not be sent to {}: {}", normalizedNumber, ex.getMessage());
            return SmsDeliveryResult.failedResult(ex.getMessage());
        }
    }

    private SmsDeliveryResult parseDeliveryResult(String responseBody, String normalizedNumber) {
        if (isBlank(responseBody)) {
            return SmsDeliveryResult.sentResult();
        }
        if (responseBody.contains("\"status\":\"Success\"")
                || responseBody.contains("\"status\": \"Success\"")
                || responseBody.contains("\"statusCode\":101")
                || responseBody.contains("\"statusCode\": 101")
                || responseBody.contains("\"statusCode\":102")
                || responseBody.contains("\"statusCode\": 102")) {
            return SmsDeliveryResult.sentResult();
        }
        return SmsDeliveryResult.failedResult("Africa's Talking rejected " + normalizedNumber + ": " + responseBody);
    }

    private boolean isConfigured() {
        return enabled && !isBlank(username) && !isBlank(apiKey) && !isBlank(smsUrl);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizePhoneNumber(String value) {
        if (isBlank(value)) {
            return null;
        }
        String number = value.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
        if (number.startsWith("+")) {
            return number;
        }
        if (number.startsWith("00")) {
            return "+" + number.substring(2);
        }
        if (number.startsWith("237")) {
            return "+" + number;
        }
        if (number.length() == 9 && number.startsWith("6")) {
            return "+237" + number;
        }
        return number;
    }

    public record SmsDeliveryResult(boolean sent, boolean skipped, String errorMessage) {
        static SmsDeliveryResult sentResult() {
            return new SmsDeliveryResult(true, false, null);
        }

        static SmsDeliveryResult skippedResult(String reason) {
            return new SmsDeliveryResult(false, true, reason);
        }

        static SmsDeliveryResult failedResult(String reason) {
            return new SmsDeliveryResult(false, false, reason);
        }
    }
}
