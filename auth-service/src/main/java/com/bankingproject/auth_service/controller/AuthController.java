package com.bankingproject.auth_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankingproject.auth_service.dto.AuthResponse;
import com.bankingproject.auth_service.dto.LoginRequest;
import com.bankingproject.auth_service.dto.RegisterRequest;
import com.bankingproject.auth_service.dto.UserResponse;
import com.bankingproject.auth_service.service.AuthService;

@RestController
@RequestMapping
public class AuthController {

	private final AuthService authService;
	private final Environment environment;

	public AuthController(AuthService authService, Environment environment) {
		this.authService = authService;
		this.environment = environment;
	}

	@GetMapping("/")
	public Map<String, Object> home() {
		return Map.of(
				"service", "auth-service",
				"port", environment.getProperty("local.server.port"),
				"status", "UP");
	}

	@GetMapping("/config")
	public Map<String, Object> config() {
		return Map.of(
				"applicationName", environment.getProperty("spring.application.name"),
				"serverPort", environment.getProperty("server.port"),
				"databaseUrl", environment.getProperty("spring.datasource.url"),
				"eurekaUrl", environment.getProperty("eureka.client.service-url.defaultZone"));
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/users")
	public List<UserResponse> users() {
		return authService.findAllUsers();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
	}
}
