package com.example.transaction_service.strategy;

import com.example.transaction_service.client.ResilientAccountClient;
import com.example.transaction_service.factory.TransactionFactory;
import com.example.transaction_service.model.Transaction;
import com.example.transaction_service.model.TransactionType;
import com.example.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferStrategyTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionFactory transactionFactory;

    @Mock
    private ResilientAccountClient accountClient;

    @InjectMocks
    private TransferStrategy transferStrategy;

    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleTransaction = new Transaction(1L, 2L, BigDecimal.valueOf(3000),
                TransactionType.TRANSFER, LocalDateTime.now());
    }

    @Test
    void transfertReussi_neDeclencheAucuneCompensation() {
        when(transactionFactory.createTransfer(1L, 2L, BigDecimal.valueOf(3000)))
                .thenReturn(sampleTransaction);
        when(transactionRepository.save(sampleTransaction)).thenReturn(sampleTransaction);

        Transaction result = transferStrategy.execute(1L, 2L, BigDecimal.valueOf(3000));

        // Débit source, crédit destination : 2 appels, aucune compensation
        verify(accountClient).updateBalance(1L, BigDecimal.valueOf(3000).negate());
        verify(accountClient).updateBalance(2L, BigDecimal.valueOf(3000));
        verify(accountClient, times(2)).updateBalance(any(), any());
        assertThatThrownBy(() -> {}).doesNotThrowAnyException(); // sanity check
    }

    @Test
    void echecCreditDestination_declencheCompensationSurSource() {
        when(transactionFactory.createTransfer(1L, 2L, BigDecimal.valueOf(3000)))
                .thenReturn(sampleTransaction);
        when(transactionRepository.save(sampleTransaction)).thenReturn(sampleTransaction);

        // Le débit source réussit, mais le crédit destination échoue
        doThrow(new RuntimeException("service-account indisponible"))
                .when(accountClient).updateBalance(eq(2L), eq(BigDecimal.valueOf(3000)));

        assertThatThrownBy(() -> transferStrategy.execute(1L, 2L, BigDecimal.valueOf(3000)))
                .isInstanceOf(RuntimeException.class);

        // Vérifie la séquence Saga complète :
        // 1. débit source (-3000)
        // 2. tentative crédit destination (échoue)
        // 3. compensation : recrédit source (+3000)
        verify(accountClient).updateBalance(1L, BigDecimal.valueOf(3000).negate());
        verify(accountClient).updateBalance(2L, BigDecimal.valueOf(3000));
        verify(accountClient).updateBalance(1L, BigDecimal.valueOf(3000)); // compensation
        verify(accountClient, times(2)).updateBalance(eq(1L), any()); // débit + compensation
    }
}