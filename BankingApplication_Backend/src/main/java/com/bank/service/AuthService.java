package com.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.OtpVerification;
import com.bank.model.PasswordResetToken;
import com.bank.repository.AccountRepository;
import com.bank.repository.OtpRepository;
import com.bank.repository.PasswordResetRepository;
import com.bank.util.EmailUtil;
import com.bank.util.EnvUtil;
import com.bank.util.TokenUtil;

public class AuthService {

	private static final String REGISTER_PURPOSE = "REGISTER";
	private static final int OTP_EXPIRY_MINUTES = 10;
	private static final int RESET_EXPIRY_MINUTES = 30;
	private static final String FRONTEND_URL = EnvUtil.getOrDefault("BANK_FRONTEND_URL", "http://localhost:5173");

	private final AccountRepository accountRepository;
	private final AccountService accountService;
	private final OtpRepository otpRepository;
	private final PasswordResetRepository passwordResetRepository;

	public AuthService(AccountRepository accountRepository, AccountService accountService, OtpRepository otpRepository,
			PasswordResetRepository passwordResetRepository) {
		this.accountRepository = accountRepository;
		this.accountService = accountService;
		this.otpRepository = otpRepository;
		this.passwordResetRepository = passwordResetRepository;
	}

	public void sendRegistrationOtp(String name, String email, String password, BigDecimal depositAmount)
			throws InvalidAmountException {
		validateRegistrationData(name, email, password, depositAmount);
		if (accountRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("An account already exists for this email.");
		}

		String otp = TokenUtil.generateOtp();
		otpRepository.save(email, otp, REGISTER_PURPOSE, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
		EmailUtil.sendEmail(email, "Bank registration OTP",
				"Hello " + name + ",\n\nYour OTP for account registration is: " + otp
						+ "\nThis code is valid for 10 minutes.");
	}

	public void verifyRegistrationOtp(String email, String otp) {
		OtpVerification latestOtp = otpRepository.findLatest(email, REGISTER_PURPOSE);
		if (latestOtp == null) {
			throw new IllegalArgumentException("No OTP request found for this email.");
		}
		if (latestOtp.getVerifiedAt() != null) {
			return;
		}
		if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("OTP has expired. Please request a new one.");
		}
		if (!latestOtp.getOtpCode().equals(otp)) {
			throw new IllegalArgumentException("Invalid OTP.");
		}
		otpRepository.markVerified(latestOtp.getId());
	}

	public Account register(String name, String email, String password, BigDecimal depositAmount)
			throws InvalidAmountException {
		validateRegistrationData(name, email, password, depositAmount);
		OtpVerification latestOtp = otpRepository.findLatest(email, REGISTER_PURPOSE);
		if (latestOtp == null || latestOtp.getVerifiedAt() == null) {
			throw new IllegalArgumentException("Email verification is required before registration.");
		}
		if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Verified OTP has expired. Request a new OTP.");
		}
		return accountService.createAccount(name, email, password, depositAmount);
	}

	public Account login(String email, String password) throws AccountNotFoundException {
		return accountService.authenticate(email, password);
	}

	public void sendUserPasswordReset(String email) {
		Account account = accountRepository.findByEmail(email);
		if (account == null) {
			throw new IllegalArgumentException("No account exists for this email.");
		}

		String token = TokenUtil.generateToken(48);
		passwordResetRepository.save(email, token, "USER", LocalDateTime.now().plusMinutes(RESET_EXPIRY_MINUTES));
		String resetLink = FRONTEND_URL + "/reset-password?token=" + token + "&type=user";
		EmailUtil.sendEmail(email, "Reset your banking password",
				"Hello " + account.getHolderName() + ",\n\nUse the link below to reset your password:\n" + resetLink
						+ "\n\nThis link is valid for 30 minutes.");
	}

	public void resetPassword(String token, String newPassword) {
		PasswordResetToken resetToken = passwordResetRepository.findValidToken(token);
		validateResetToken(resetToken);
		if (!"USER".equalsIgnoreCase(resetToken.getUserType())) {
			throw new IllegalArgumentException("This reset link is not for a user account.");
		}
		accountService.updatePassword(resetToken.getEmail(), newPassword);
		passwordResetRepository.markUsed(resetToken.getId());
	}

	private void validateRegistrationData(String name, String email, String password, BigDecimal depositAmount)
			throws InvalidAmountException {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Name is required.");
		}
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required.");
		}
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Password is required.");
		}
		if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidAmountException("Deposit amount must be zero or more.");
		}
	}

	protected void validateResetToken(PasswordResetToken resetToken) {
		if (resetToken == null) {
			throw new IllegalArgumentException("Invalid reset link.");
		}
		if (resetToken.getUsedAt() != null) {
			throw new IllegalArgumentException("This reset link has already been used.");
		}
		if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("This reset link has expired.");
		}
	}
}
