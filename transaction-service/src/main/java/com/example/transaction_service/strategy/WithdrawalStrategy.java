package com.example.transaction_service.strategy;

import com.example.transaction_service.client.ResilientAccountClient;
import com.example.transaction_service.factory.TransactionFactory;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WithdrawalStrategy implements TransactionStrategy {

    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final ResilientAccountClient accountClient;

    public WithdrawalStrategy(TransactionRepository transactionRepository,
                               TransactionFactory transactionFactory,
                               ResilientAccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.transactionFactory = transactionFactory;
        this.accountClient = accountClient;
    }

    @Override
    public Transaction execute(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction transaction = transactionFactory.createWithdrawal(sourceAccountId, amount);
        Transaction saved = transactionRepository.save(transaction);
        accountClient.updateBalance(sourceAccountId, amount.negate());
        return saved;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.WITHDRAWAL;
    }
}