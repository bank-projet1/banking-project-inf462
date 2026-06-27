package com.bankingproject.service_account.repository;

import com.bankingproject.service_account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByCustomerId(Long customerId);

    Optional<Account> findFirstByCustomerIdAndStatusOrderByIdAsc(Long customerId, String status);

    Optional<Account> findFirstByPhoneNumberAndStatusOrderByIdAsc(String phoneNumber, String status);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
