package com.example.transaction_service.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCreatedEvent(
        Long transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String type,
        LocalDateTime timestamp
) {}