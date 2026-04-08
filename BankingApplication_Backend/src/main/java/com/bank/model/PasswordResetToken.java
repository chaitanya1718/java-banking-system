package com.bank.model;

import java.time.LocalDateTime;

public class PasswordResetToken {

	private long id;
	private String email;
	private String token;
	private String userType;
	private LocalDateTime expiresAt;
	private LocalDateTime usedAt;
	private LocalDateTime createdAt;

	public PasswordResetToken(long id, String email, String token, String userType, LocalDateTime expiresAt,
			LocalDateTime usedAt, LocalDateTime createdAt) {
		this.id = id;
		this.email = email;
		this.token = token;
		this.userType = userType;
		this.expiresAt = expiresAt;
		this.usedAt = usedAt;
		this.createdAt = createdAt;
	}

	public long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getToken() {
		return token;
	}

	public String getUserType() {
		return userType;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public LocalDateTime getUsedAt() {
		return usedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
