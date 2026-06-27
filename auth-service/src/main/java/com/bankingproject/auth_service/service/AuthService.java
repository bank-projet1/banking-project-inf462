package com.bankingproject.auth_service.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankingproject.auth_service.dto.AuthResponse;
import com.bankingproject.auth_service.dto.LoginRequest;
import com.bankingproject.auth_service.dto.RegisterRequest;
import com.bankingproject.auth_service.dto.UserResponse;
import com.bankingproject.auth_service.entity.AppUser;
import com.bankingproject.auth_service.entity.UserRole;
import com.bankingproject.auth_service.repository.AppUserRepository;

@Service
public class AuthService {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("Un utilisateur existe deja avec cet email.");
		}

		AppUser user = new AppUser();
		user.setFullName(request.fullName());
		user.setEmail(request.email());
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setRole(request.role() == null ? UserRole.CLIENT : request.role());

		return toResponse(userRepository.save(user));
	}

	public AuthResponse login(LoginRequest request) {
		AppUser user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe invalide."));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new IllegalArgumentException("Email ou mot de passe invalide.");
		}

		String token = jwtService.generateToken(user);
		return new AuthResponse(token, "Bearer", user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
	}

	public List<UserResponse> findAllUsers() {
		return userRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	public UserResponse findUserById(Long id) {
		return userRepository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
	}

	@Transactional
	public UserResponse updateUser(Long id, RegisterRequest request) {
		AppUser user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

		userRepository.findByEmail(request.email())
				.filter(existingUser -> !existingUser.getId().equals(id))
				.ifPresent(existingUser -> {
					throw new IllegalArgumentException("Un utilisateur existe deja avec cet email.");
				});

		user.setFullName(request.fullName());
		user.setEmail(request.email());

		if (request.password() != null && !request.password().isBlank()) {
			user.setPassword(passwordEncoder.encode(request.password()));
		}

		user.setRole(request.role() == null ? user.getRole() : request.role());

		return toResponse(userRepository.save(user));
	}

	@Transactional
	public void deleteUser(Long id) {
		if (!userRepository.existsById(id)) {
			throw new IllegalArgumentException("Utilisateur introuvable.");
		}

		userRepository.deleteById(id);
	}

	private UserResponse toResponse(AppUser user) {
		return new UserResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getRole().name(),
				user.isEnabled(),
				user.getCreatedAt());
	}
}
