package com.bank.model;

import java.time.LocalDateTime;

public class OtpVerification {

	private long id;
	private String email;
	private String otpCode;
	private String purpose;
	private LocalDateTime expiresAt;
	private LocalDateTime verifiedAt;
	private LocalDateTime createdAt;

	public OtpVerification(long id, String email, String otpCode, String purpose, LocalDateTime expiresAt,
			LocalDateTime verifiedAt, LocalDateTime createdAt) {
		this.id = id;
		this.email = email;
		this.otpCode = otpCode;
		this.purpose = purpose;
		this.expiresAt = expiresAt;
		this.verifiedAt = verifiedAt;
		this.createdAt = createdAt;
	}

	public long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getOtpCode() {
		return otpCode;
	}

	public String getPurpose() {
		return purpose;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public LocalDateTime getVerifiedAt() {
		return verifiedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
