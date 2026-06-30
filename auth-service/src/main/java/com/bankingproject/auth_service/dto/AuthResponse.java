package com.bankingproject.auth_service.dto;

public record AuthResponse(
		String token,
		String tokenType,
		Long userId,
		String fullName,
		String email,
		String role) {
}
