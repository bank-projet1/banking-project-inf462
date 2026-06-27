package com.bankingproject.auth_service.dto;

import com.bankingproject.auth_service.entity.UserRole;

public record RegisterRequest(
		String fullName,
		String email,
		String phoneNumber,
		String password,
		UserRole role) {
}
