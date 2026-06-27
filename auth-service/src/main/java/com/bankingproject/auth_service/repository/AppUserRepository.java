package com.bankingproject.auth_service.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bankingproject.auth_service.entity.AppUser;
import com.bankingproject.auth_service.entity.UserRole;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmail(String email);

	Optional<AppUser> findByPhoneNumberAndEnabledTrue(String phoneNumber);

	boolean existsByEmail(String email);

	List<AppUser> findByFullNameContainingIgnoreCaseAndEnabledTrue(String fullName);

	List<AppUser> findByRoleAndEnabledTrue(UserRole role);
}
