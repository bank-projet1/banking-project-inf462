package com.example.transaction_service.factory;

import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransactionFactory {

    public Transaction createDeposit(Long accountId, BigDecimal amount) {
        return new Transaction(null, accountId, amount, TransactionType.DEPOSIT, LocalDateTime.now());
    }

    public Transaction createWithdrawal(Long accountId, BigDecimal amount) {
        return new Transaction(accountId, null, amount, TransactionType.WITHDRAWAL, LocalDateTime.now());
    }

    public Transaction createTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        return new Transaction(sourceAccountId, destinationAccountId, amount, TransactionType.TRANSFER, LocalDateTime.now());
    }
}