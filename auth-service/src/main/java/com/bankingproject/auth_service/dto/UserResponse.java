package com.bankingproject.auth_service.dto;

import java.time.LocalDateTime;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		String role,
		boolean enabled,
		LocalDateTime createdAt) {
}
