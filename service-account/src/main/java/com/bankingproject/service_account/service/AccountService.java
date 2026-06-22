package com.bankingproject.service_account.service;

import com.bankingproject.service_account.config.RabbitMQConfig;
import com.bankingproject.service_account.dto.NotificationMessage;
import com.bankingproject.service_account.model.Account;
import com.bankingproject.service_account.repository.AccountRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public AccountService(AccountRepository repository,
                          RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Account createAccount(Account account) {

        Account saved = repository.save(account);

        NotificationMessage notification =
                new NotificationMessage(
                        saved.getId(),
                        "ACCOUNT_CREATED",
                        "Compte créé avec succès : "
                                + saved.getAccountNumber()
                );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_QUEUE,
                notification
        );

        return saved;
    }
}