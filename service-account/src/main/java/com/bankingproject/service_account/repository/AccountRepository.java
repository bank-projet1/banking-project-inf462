package com.bankingproject.service_account.repository;

import com.bankingproject.service_account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository
        extends JpaRepository<Account, Long> {
}