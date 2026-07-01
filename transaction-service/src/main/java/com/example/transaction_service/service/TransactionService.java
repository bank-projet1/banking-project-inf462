package com.example.transaction_service.service;

import com.example.transaction_service.event.TransactionEventPublisher;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.strategy.TransactionStrategyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStrategyResolver strategyResolver;
    private final TransactionEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
                               TransactionStrategyResolver strategyResolver,
                               TransactionEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.strategyResolver = strategyResolver;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount) {
        Transaction saved = strategyResolver.resolve(TransactionType.DEPOSIT).execute(null, accountId, amount);
        eventPublisher.publishCreated(saved);
        return saved;
    }

    @Transactional
    public Transaction withdrawal(Long accountId, BigDecimal amount) {
        Transaction saved = strategyResolver.resolve(TransactionType.WITHDRAWAL).execute(accountId, null, amount);
        eventPublisher.publishCreated(saved);
        return saved;
    }

    @Transactional
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction saved = strategyResolver.resolve(TransactionType.TRANSFER)
                .execute(sourceAccountId, destinationAccountId, amount);
        eventPublisher.publishCreated(saved);
        return saved;
    }

    public List<Transaction> getAccountHistory(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrDestinationAccountId(accountId, accountId);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    @Transactional
    public Transaction updateTransaction(Long id, Transaction transaction) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        existing.setSourceAccountId(transaction.getSourceAccountId());
        existing.setDestinationAccountId(transaction.getDestinationAccountId());
        existing.setAmount(transaction.getAmount());
        existing.setType(transaction.getType());
        existing.setTimestamp(transaction.getTimestamp() == null ? existing.getTimestamp() : transaction.getTimestamp());
        return transactionRepository.save(existing);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found");
        }
        transactionRepository.deleteById(id);
    }
}