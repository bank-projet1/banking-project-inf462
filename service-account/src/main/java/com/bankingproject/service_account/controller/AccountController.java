package com.bankingproject.service_account.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankingproject.service_account.model.Account;
import com.bankingproject.service_account.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(
            AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public Account createAccount(
            @RequestBody Account account) {

        return accountService.createAccount(account);
    }
}