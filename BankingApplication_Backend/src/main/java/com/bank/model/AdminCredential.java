package com.bank.model;

import java.time.LocalDateTime;

public class AdminCredential {

	private String email;
	private String passwordHash;
	private String displayName;
	private LocalDateTime updatedAt;

	public AdminCredential(String email, String passwordHash, String displayName, LocalDateTime updatedAt) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.updatedAt = updatedAt;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
