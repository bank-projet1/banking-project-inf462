package com.example.transaction_service.service;

import com.example.transaction_service.client.AccountClient;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public TransactionService(TransactionRepository transactionRepository, AccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction(null, accountId, amount, TransactionType.DEPOSIT, LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Appele le microservice compte pour ajouter l'argent
        accountClient.updateBalance(accountId, amount);

        return savedTransaction;
    }

    @Transactional
    public Transaction withdrawal(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction(accountId, null, amount, TransactionType.WITHDRAWAL, LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Appele le microservice compte pour retirer l'argent (montant négatif)
        accountClient.updateBalance(accountId, amount.negate());

        return savedTransaction;
    }

    @Transactional
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction(sourceAccountId, destinationAccountId, amount, TransactionType.TRANSFER, LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 1. Débiter le compte source (montant négatif)
        accountClient.updateBalance(sourceAccountId, amount.negate());

        // 2. Créditer le compte destination (montant positif)
        accountClient.updateBalance(destinationAccountId, amount);

        return savedTransaction;
    }

    public List<Transaction> getAccountHistory(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrDestinationAccountId(accountId, accountId);
    }
}