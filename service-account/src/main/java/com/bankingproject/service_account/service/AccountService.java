package com.bankingproject.service_account.service;

import com.bankingproject.service_account.entity.Account;
import com.bankingproject.service_account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account create(Account account) {
        if (account.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            account.setAccountNumber(generateAccountNumber());
        }
        if (account.getBalance() == null) {
            account.setBalance(0.0);
        }
        if (account.getStatus() == null || account.getStatus().isBlank()) {
            account.setStatus("ACTIVE");
        }
        if (account.getCurrency() == null || account.getCurrency().isBlank()) {
            account.setCurrency("XAF");
        }
        if (account.getAccountType() == null || account.getAccountType().isBlank()) {
            account.setAccountType("CURRENT");
        }
        return accountRepository.save(account);
    }

    public List<Account> getAll() {
        return accountRepository.findAll();
    }

    public Optional<Account> getById(Long id) {
        return accountRepository.findById(id);
    }

    public List<Account> getByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public Account getDefaultActiveAccount(Long customerId) {
        return accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(customerId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("No active account found for customer " + customerId));
    }

    public Account updateBalance(Long id, BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        BigDecimal currentBalance = BigDecimal.valueOf(account.getBalance() == null ? 0.0 : account.getBalance());
        account.setBalance(currentBalance.add(amount).doubleValue());
        return accountRepository.save(account);
    }

    public void delete(Long id) {
        accountRepository.deleteById(id);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "BP" + System.currentTimeMillis()
                    + ThreadLocalRandom.current().nextInt(100, 1000);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
