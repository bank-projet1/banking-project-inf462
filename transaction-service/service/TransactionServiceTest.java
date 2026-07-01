package com.example.transaction_service.service;

import com.example.transaction_service.event.TransactionEventPublisher;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.strategy.TransactionStrategy;
import com.example.transaction_service.strategy.TransactionStrategyResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionStrategyResolver strategyResolver;

    @Mock
    private TransactionEventPublisher eventPublisher;

    @Mock
    private TransactionStrategy depositStrategy;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void depotReussi_publieUnEvenement() {
        Transaction transaction = new Transaction(null, 1L, BigDecimal.valueOf(5000),
                TransactionType.DEPOSIT, LocalDateTime.now());

        when(strategyResolver.resolve(TransactionType.DEPOSIT)).thenReturn(depositStrategy);
        when(depositStrategy.execute(null, 1L, BigDecimal.valueOf(5000))).thenReturn(transaction);

        transactionService.deposit(1L, BigDecimal.valueOf(5000));

        verify(eventPublisher).publishCreated(transaction);
    }

    @Test
    void depotEnEchec_nePublieAucunEvenement() {
        when(strategyResolver.resolve(TransactionType.DEPOSIT)).thenReturn(depositStrategy);
        when(depositStrategy.execute(null, 1L, BigDecimal.valueOf(5000)))
                .thenThrow(new RuntimeException("service-account indisponible"));

        assertThatThrownBy(() -> transactionService.deposit(1L, BigDecimal.valueOf(5000)))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(eventPublisher);
    }
}