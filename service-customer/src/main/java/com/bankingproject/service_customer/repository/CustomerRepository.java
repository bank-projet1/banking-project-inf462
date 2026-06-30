package com.bankingproject.service_customer.repository;

import com.bankingproject.service_customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}