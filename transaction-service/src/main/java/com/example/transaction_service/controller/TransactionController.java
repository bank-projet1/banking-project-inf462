package com.example.transaction_service.controller;

import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.dto.AccountResponse;
import com.example.transaction_service.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Endpoint pour faire un dépôt
     * POST http://localhost:8085/api/transactions/deposit?accountId=1&amount=50000
     */
    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(
            @RequestParam Long accountId, 
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.deposit(accountId, amount));
    }

    /**
     * Endpoint pour faire un retrait
     * POST http://localhost:8085/api/transactions/withdrawal?accountId=1&amount=10000
     */
    @PostMapping("/withdrawal")
    public ResponseEntity<Transaction> withdrawal(
            @RequestParam Long accountId, 
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.withdrawal(accountId, amount));
    }

    /**
     * Endpoint pour faire un virement de compte à compte
     * POST http://localhost:8085/api/transactions/transfer?sourceAccountId=1&destinationAccountId=2&amount=15000
     */
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(
            @RequestParam Long sourceAccountId,
            @RequestParam Long destinationAccountId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.transfer(sourceAccountId, destinationAccountId, amount));
    }

    @PostMapping("/transfer/customer")
    public ResponseEntity<Transaction> transferToCustomer(
            @RequestParam Long sourceAccountId,
            @RequestParam Long receiverCustomerId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.transferToCustomer(sourceAccountId, receiverCustomerId, amount));
    }

    @PostMapping("/transfer/phone")
    public ResponseEntity<Transaction> transferToCustomerPhoneNumber(
            @RequestParam Long sourceAccountId,
            @RequestParam String receiverPhoneNumber,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.transferToCustomerPhoneNumber(sourceAccountId, receiverPhoneNumber, amount));
    }

    @GetMapping("/receiver/{customerId}/account")
    public ResponseEntity<AccountResponse> getReceiverAccount(@PathVariable Long customerId) {
        return ResponseEntity.ok(transactionService.getDefaultAccountByCustomer(customerId));
    }

    @GetMapping("/receiver/account")
    public ResponseEntity<AccountResponse> getReceiverAccountByPhoneNumber(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(transactionService.getDefaultAccountByCustomerPhoneNumber(phoneNumber));
    }

    /**
     * Endpoint pour récupérer l'historique d'un compte
     * GET http://localhost:8085/api/transactions/history/1
     */
    @GetMapping("/history/{accountId}")
    public ResponseEntity<List<Transaction>> getHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getAccountHistory(accountId));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
}
