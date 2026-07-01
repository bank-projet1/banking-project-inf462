package com.example.transaction_service.strategy;

import com.example.transaction_service.client.ResilientAccountClient;
import com.example.transaction_service.factory.TransactionFactory;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferStrategy implements TransactionStrategy {

    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final ResilientAccountClient accountClient;

    public TransferStrategy(TransactionRepository transactionRepository,
                             TransactionFactory transactionFactory,
                             ResilientAccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.transactionFactory = transactionFactory;
        this.accountClient = accountClient;
    }

    @Override
    public Transaction execute(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction transaction = transactionFactory.createTransfer(sourceAccountId, destinationAccountId, amount);
        Transaction saved = transactionRepository.save(transaction);

        // Étape 1 : débit du compte source
        accountClient.updateBalance(sourceAccountId, amount.negate());

        // Étape 2 : crédit du compte destination — compensation Saga si échec
        try {
            accountClient.updateBalance(destinationAccountId, amount);
        } catch (Exception e) {
            accountClient.updateBalance(sourceAccountId, amount); // compensation
            throw e;
        }

        return saved;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.TRANSFER;
    }
}