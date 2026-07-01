package com.example.transaction_service.strategy;

import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;

import java.math.BigDecimal;

public interface TransactionStrategy {
    Transaction execute(Long sourceAccountId, Long destinationAccountId, BigDecimal amount);
    TransactionType getType();
}