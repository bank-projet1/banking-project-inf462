package com.bankingproject.loanservice.client;

import com.bankingproject.loanservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Component
public class TransactionClient {

    private final RestTemplate restTemplate;
    private final String transactionServiceUrl;

    public TransactionClient(
            RestTemplate restTemplate,
            @Value("${external.service.transaction-url:http://localhost:8085}") String transactionServiceUrl) {
        this.restTemplate = restTemplate;
        this.transactionServiceUrl = transactionServiceUrl;
    }

    public void deposit(Long accountId, BigDecimal amount) {
        postTransaction("/api/transactions/deposit", accountId, amount);
    }

    public void withdrawal(Long accountId, BigDecimal amount) {
        postTransaction("/api/transactions/withdrawal", accountId, amount);
    }

    private void postTransaction(String path, Long accountId, BigDecimal amount) {
        String url = UriComponentsBuilder
                .fromHttpUrl(transactionServiceUrl + path)
                .queryParam("accountId", accountId)
                .queryParam("amount", amount)
                .toUriString();
        try {
            restTemplate.postForEntity(url, null, Void.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach transaction-service at " + transactionServiceUrl, ex);
        }
    }
}
