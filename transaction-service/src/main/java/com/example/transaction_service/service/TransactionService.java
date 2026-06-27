package com.example.transaction_service.service;

import com.example.transaction_service.client.AuthClient;
import com.example.transaction_service.client.AccountClient;
import com.example.transaction_service.client.NotificationClient;
import com.example.transaction_service.dto.AccountResponse;
import com.example.transaction_service.dto.NotificationRequest;
import com.example.transaction_service.dto.UserResponse;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final AuthClient authClient;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountClient accountClient,
            NotificationClient notificationClient,
            AuthClient authClient) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
        this.notificationClient = notificationClient;
        this.authClient = authClient;
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount) {
        validatePositiveAmount(amount);
        AccountResponse account = requireActiveAccount(accountId);

        accountClient.updateBalance(accountId, amount);

        Transaction transaction = new Transaction(null, accountId, amount, TransactionType.DEPOSIT, LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);
        notifyUser(account.getCustomerId(),
                "Depot reussi de " + amount + " sur le compte " + safeAccountNumber(account) + ".",
                account.getPhoneNumber());

        return savedTransaction;
    }

    @Transactional
    public Transaction withdrawal(Long accountId, BigDecimal amount) {
        validatePositiveAmount(amount);
        AccountResponse account = requireActiveAccount(accountId);
        ensureSufficientBalance(account, amount);

        accountClient.updateBalance(accountId, amount.negate());

        Transaction transaction = new Transaction(accountId, null, amount, TransactionType.WITHDRAWAL, LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);
        notifyUser(account.getCustomerId(),
                "Retrait reussi de " + amount + " depuis le compte " + safeAccountNumber(account) + ".",
                account.getPhoneNumber());

        return savedTransaction;
    }

    @Transactional
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        validatePositiveAmount(amount);
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        AccountResponse sourceAccount = requireActiveAccount(sourceAccountId);
        AccountResponse destinationAccount = requireActiveAccount(destinationAccountId);
        ensureSufficientBalance(sourceAccount, amount);

        accountClient.updateBalance(sourceAccountId, amount.negate());
        accountClient.updateBalance(destinationAccountId, amount);

        Transaction transaction = new Transaction(sourceAccountId, destinationAccountId, amount, TransactionType.TRANSFER, LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);
        notifyTransfer(sourceAccount, destinationAccount, amount);

        return savedTransaction;
    }

    @Transactional
    public Transaction transferToCustomer(Long sourceAccountId, Long receiverCustomerId, BigDecimal amount) {
        AccountResponse destinationAccount = accountClient.getDefaultAccountByCustomer(receiverCustomerId);
        return transfer(sourceAccountId, destinationAccount.getId(), amount);
    }

    @Transactional
    public Transaction transferToCustomerPhoneNumber(Long sourceAccountId, String receiverPhoneNumber, BigDecimal amount) {
        AccountResponse destinationAccount = getDefaultAccountByCustomerPhoneNumber(receiverPhoneNumber);
        return transfer(sourceAccountId, destinationAccount.getId(), amount);
    }

    public List<Transaction> getAccountHistory(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrDestinationAccountIdOrderByTimestampDesc(accountId, accountId);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    public AccountResponse getDefaultAccountByCustomer(Long customerId) {
        return accountClient.getDefaultAccountByCustomer(customerId);
    }

    public AccountResponse getDefaultAccountByCustomerPhoneNumber(String receiverPhoneNumber) {
        if (receiverPhoneNumber == null || receiverPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("Receiver phone number is required");
        }
        return accountClient.getDefaultAccountByPhoneNumber(receiverPhoneNumber.trim());
    }

    private AccountResponse requireActiveAccount(Long accountId) {
        AccountResponse account = accountClient.getAccount(accountId);
        if (account == null || account.getId() == null) {
            throw new IllegalArgumentException("Account not found");
        }
        if (account.getStatus() != null && !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Account " + accountId + " is not active");
        }
        return account;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void ensureSufficientBalance(AccountResponse account, BigDecimal amount) {
        BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance on account " + account.getId());
        }
    }

    private void notifyTransfer(AccountResponse sourceAccount, AccountResponse destinationAccount, BigDecimal amount) {
        String sourceNumber = safeAccountNumber(sourceAccount);
        String destinationNumber = safeAccountNumber(destinationAccount);
        String sourcePhoneNumber = firstPresent(findUserPhoneNumber(sourceAccount.getCustomerId()), sourceAccount.getPhoneNumber());
        String destinationPhoneNumber = firstPresent(destinationAccount.getPhoneNumber(), findUserPhoneNumber(destinationAccount.getCustomerId()));
        notifyUser(sourceAccount.getCustomerId(),
                "Virement reussi de " + amount + " vers " + safeOwnerName(destinationAccount) + " (" + destinationNumber + ").",
                sourcePhoneNumber);
        notifyUser(destinationAccount.getCustomerId(),
                "Reception reussie de " + amount + " depuis le compte " + sourceNumber + ".",
                destinationPhoneNumber);
        notifyAdministrators("Audit transaction: virement de " + amount
                + " du compte " + sourceNumber
                + " vers le compte " + destinationNumber + ".");
    }

    private void notifyUser(Long userId, String message, String phoneNumber) {
        if (userId == null) {
            return;
        }
        try {
            notificationClient.createNotification(new NotificationRequest(userId, "SMS", message, "SUCCESS", phoneNumber));
        } catch (RuntimeException error) {
            log.warn("Notification could not be sent to user {}: {}", userId, error.getMessage());
        }
    }

    private void notifyAdministrators(String message) {
        try {
            List<UserResponse> admins = authClient.findAdministrators();
            for (UserResponse admin : admins) {
                if (admin.getId() != null) {
                    notificationClient.createNotification(new NotificationRequest(admin.getId(), "AUDIT", message, "SUCCESS", admin.getPhoneNumber()));
                }
            }
        } catch (RuntimeException error) {
            log.warn("Admin notification could not be sent: {}", error.getMessage());
        }
    }

    private String findUserPhoneNumber(Long userId) {
        try {
            UserResponse user = authClient.findUserById(userId);
            return user == null ? null : user.getPhoneNumber();
        } catch (RuntimeException error) {
            log.warn("Phone number could not be loaded for user {}: {}", userId, error.getMessage());
            return null;
        }
    }

    private String safeAccountNumber(AccountResponse account) {
        return account.getAccountNumber() == null || account.getAccountNumber().isBlank()
                ? "#" + account.getId()
                : account.getAccountNumber();
    }

    private String safeOwnerName(AccountResponse account) {
        return account.getOwnerName() == null || account.getOwnerName().isBlank()
                ? "beneficiaire"
                : account.getOwnerName();
    }

    private String firstPresent(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
