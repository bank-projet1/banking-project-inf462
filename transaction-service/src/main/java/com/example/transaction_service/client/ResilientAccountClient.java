package com.example.transaction_service.client;

import com.example.transaction_service.exception.AccountServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ResilientAccountClient {

    private final AccountClient accountClient;

    public ResilientAccountClient(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackUpdateBalance")
    @Retry(name = "accountService")
    public Map<String, Object> updateBalance(Long accountId, BigDecimal amount) {
        return accountClient.updateBalance(accountId, amount);
    }

    private Map<String, Object> fallbackUpdateBalance(Long accountId, BigDecimal amount, Throwable t) {
        throw new AccountServiceUnavailableException(
                "service-account indisponible pour le compte " + accountId, t);
    }
}