package com.example.transaction_service.client;

import com.example.transaction_service.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "service-account", url = "${external.service.account-url:http://localhost:8082}")
public interface AccountClient {

    @GetMapping("/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable("accountId") Long accountId);

    @GetMapping("/accounts/customer/{customerId}/default")
    AccountResponse getDefaultAccountByCustomer(@PathVariable("customerId") Long customerId);

    @PutMapping("/accounts/update-balance")
    Map<String, Object> updateBalance(
            @RequestParam("accountId") Long accountId, 
            @RequestParam("amount") BigDecimal amount
    );
}
