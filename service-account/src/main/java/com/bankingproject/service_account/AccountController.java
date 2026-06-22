package com.bankingproject.service_account;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

	private final Environment environment;

	@Value("${bank.message:Configuration distante non chargee}")
	private String message;

	@Value("${bank.currency:XAF}")
	private String currency;

	public AccountController(Environment environment) {
		this.environment = environment;
	}

	@GetMapping("/")
	public Map<String, Object> home() {
		return Map.of(
				"service", "service-account",
				"message", message,
				"port", environment.getProperty("local.server.port"));
	}

	@GetMapping("/accounts")
	public List<Map<String, Object>> accounts() {
		return List.of(
				Map.of("id", 1, "owner", "Alice", "balance", 150000, "currency", currency),
				Map.of("id", 2, "owner", "Bob", "balance", 85000, "currency", currency));
	}

	@GetMapping("/config")
	public Map<String, Object> config() {
		return Map.of(
				"applicationName", environment.getProperty("spring.application.name"),
				"serverPort", environment.getProperty("server.port"),
				"eurekaUrl", environment.getProperty("eureka.client.service-url.defaultZone"),
				"message", message,
				"currency", currency);
	}
}
