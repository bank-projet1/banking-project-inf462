package com.example.transaction_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long sourceAccountId;      // Compte débité (null si DEPOSIT)
    private Long destinationAccountId; // Compte crédité (null si WITHDRAWAL)
    
    @Column(nullable = false)
    private BigDecimal amount;         // Utilisation de BigDecimal pour la précision financière
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;      // DEPOSIT, WITHDRAWAL, TRANSFER
    
    private LocalDateTime timestamp;

    // Constructeur par défaut (Obligatoire pour JPA)
    public Transaction() {}

    // Constructeur complet
    public Transaction(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, TransactionType type, LocalDateTime timestamp) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}