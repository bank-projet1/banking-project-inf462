package com.bankingproject.loanservice.client;

import com.bankingproject.loanservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Component
public class AccountClient {

    private final RestTemplate restTemplate;
    private final String accountServiceUrl;

    public AccountClient(RestTemplate restTemplate,
                         @Value("${external.service.account-url:http://localhost:8082}") String accountServiceUrl) {
        this.restTemplate = restTemplate;
        this.accountServiceUrl = accountServiceUrl;
    }

    public AccountDTO getAccountById(Long accountId) {
        try {
            return restTemplate.getForObject(accountServiceUrl + "/accounts/{id}", AccountDTO.class, accountId);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach service-account at " + accountServiceUrl, ex);
        }
    }

    public void updateBalance(Long accountId, BigDecimal amount) {
        String url = UriComponentsBuilder
                .fromHttpUrl(accountServiceUrl + "/accounts/update-balance")
                .queryParam("accountId", accountId)
                .queryParam("amount", amount)
                .toUriString();
        try {
            restTemplate.put(url, null);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to update account balance at " + accountServiceUrl, ex);
        }
    }
}
