package com.bankingproject.loanservice.client;

import com.bankingproject.loanservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Objects;

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
            AccountDTO[] accounts = restTemplate.getForObject(accountServiceUrl + "/accounts", AccountDTO[].class);
            if (accounts == null) {
                return null;
            }
            return Arrays.stream(accounts)
                    .filter(account -> Objects.equals(accountId, account.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach service-account at " + accountServiceUrl, ex);
        }
    }
}
