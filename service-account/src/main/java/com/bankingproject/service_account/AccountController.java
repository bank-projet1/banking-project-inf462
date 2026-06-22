package com.bankingproject.service_account;
import com.bankingproject.service_account.service.AccountService;

import com.bankingproject.service_account.entity.Account;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.create(account);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAll();
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @GetMapping("/{id}/balance")
    public Map<String, Object> getBalance(@PathVariable Long id) {

        Account account = accountService.getById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return Map.of(
                "accountId", account.getId(),
                "balance", account.getBalance()
        );
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
    }
}