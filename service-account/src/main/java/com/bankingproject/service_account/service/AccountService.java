package com.bankingproject.service_account.service;

import com.bankingproject.service_account.entity.Account;
import com.bankingproject.service_account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public Account create(Account account) {
        return repository.save(account);
    }

    public List<Account> getAll() {
        return repository.findAll();
    }

    public Optional<Account> getById(Long id) {
        return repository.findById(id);
    }

    public List<Account> getByCustomerId(Long customerId) {
        return repository.findByCustomerId(customerId);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}