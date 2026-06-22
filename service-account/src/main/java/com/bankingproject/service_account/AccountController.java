package com.bankingproject.service_account;

import com.bankingproject.service_account.entity.Account;
import com.bankingproject.service_account.service.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Créer un compte
     */
    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.create(account);
    }

    /**
     * Récupérer tous les comptes
     */
    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAll();
    }

    /**
     * Récupérer un compte par ID
     */
    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    /**
     * Récupérer les comptes d'un client
     */
    @GetMapping("/customer/{customerId}")
    public List<Account> getAccountsByCustomerId(@PathVariable Long customerId) {
        return accountService.getByCustomerId(customerId);
    }

    /**
     * Supprimer un compte
     */
    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
    }
}