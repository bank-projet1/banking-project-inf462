package com.example.transaction_service.strategy;

import com.example.transaction_service.client.ResilientAccountClient;
import com.example.transaction_service.factory.TransactionFactory;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositStrategyTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionFactory transactionFactory;

    @Mock
    private ResilientAccountClient accountClient;

    @InjectMocks
    private DepositStrategy depositStrategy;

    @Test
    void depot_appelleAccountClientAvecMontantPositif() {
        Transaction transaction = new Transaction(null, 1L, BigDecimal.valueOf(5000),
                TransactionType.DEPOSIT, LocalDateTime.now());

        when(transactionFactory.createDeposit(1L, BigDecimal.valueOf(5000))).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = depositStrategy.execute(null, 1L, BigDecimal.valueOf(5000));

        verify(accountClient).updateBalance(1L, BigDecimal.valueOf(5000));
        assertThat(result).isEqualTo(transaction);
        assertThat(depositStrategy.getType()).isEqualTo(TransactionType.DEPOSIT);
    }
}