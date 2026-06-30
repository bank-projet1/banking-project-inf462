package com.example.transaction_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "service-account") 
public interface AccountClient {

    @PutMapping("/accounts/update-balance")
    Map<String, Object> updateBalance(
            @RequestParam("accountId") Long accountId, 
            @RequestParam("amount") BigDecimal amount
    );
}