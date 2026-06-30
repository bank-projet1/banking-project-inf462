package com.bankingproject.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bankingproject.auth_service.entity.AppUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${security.jwt.secret}")
	private String jwtSecret;

	@Value("${security.jwt.expiration-ms:86400000}")
	private long expirationMs;

	public String generateToken(AppUser user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("userId", user.getId())
				.claim("fullName", user.getFullName())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(secretKey())
				.compact();
	}

	private SecretKey secretKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}
}
