package com.example.transaction_service.strategy;

import com.example.transaction_service.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionStrategyResolver {

    private final Map<TransactionType, TransactionStrategy> strategies;

    public TransactionStrategyResolver(List<TransactionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TransactionStrategy::getType, Function.identity()));
    }

    public TransactionStrategy resolve(TransactionType type) {
        TransactionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Aucune stratégie pour le type : " + type);
        }
        return strategy;
    }
}