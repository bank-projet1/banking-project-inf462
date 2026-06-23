package com.example.transaction_service.service;

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

    // Injection par constructeur
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Effectuer un dépôt sur un compte
     */
    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction(
                null, 
                accountId, 
                amount, 
                TransactionType.DEPOSIT, 
                LocalDateTime.now()
        );
        return transactionRepository.save(transaction);
    }

    /**
     * Effectuer un retrait depuis un compte
     */
    @Transactional
    public Transaction withdrawal(Long accountId, BigDecimal amount) {
        Transaction transaction = new Transaction(
                accountId, 
                null, 
                amount, 
                TransactionType.WITHDRAWAL, 
                LocalDateTime.now()
        );
        return transactionRepository.save(transaction);
    }

    /**
     * Effectuer un virement de compte à compte
     */
    @Transactional
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction(
                sourceAccountId, 
                destinationAccountId, 
                amount, 
                TransactionType.TRANSFER, 
                LocalDateTime.now()
        );
        return transactionRepository.save(transaction);
    }

    /**
     * Récupérer l'historique des transactions d'un compte
     */
    public List<Transaction> getAccountHistory(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrDestinationAccountId(accountId, accountId);
    }
}