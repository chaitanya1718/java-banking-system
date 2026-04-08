package com.bank.service;

import java.time.LocalDateTime;
import java.util.Collection;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.model.Account;
import com.bank.model.AdminCredential;
import com.bank.model.PasswordResetToken;
import com.bank.repository.AccountRepository;
import com.bank.repository.AdminRepository;
import com.bank.repository.PasswordResetRepository;
import com.bank.util.EmailUtil;
import com.bank.util.EnvUtil;
import com.bank.util.PasswordUtil;
import com.bank.util.TokenUtil;

public class AdminService {

	private static final int RESET_EXPIRY_MINUTES = 30;
	private static final String FRONTEND_URL = EnvUtil.getOrDefault("BANK_FRONTEND_URL", "http://localhost:5173");

	private final AdminRepository adminRepository;
	private final AccountRepository accountRepository;
	private final AccountService accountService;
	private final PasswordResetRepository passwordResetRepository;

	public AdminService(AdminRepository adminRepository, AccountRepository accountRepository, AccountService accountService,
			PasswordResetRepository passwordResetRepository) {
		this.adminRepository = adminRepository;
		this.accountRepository = accountRepository;
		this.accountService = accountService;
		this.passwordResetRepository = passwordResetRepository;
	}

	public AdminCredential login(String email, String password) {
		AdminCredential admin = adminRepository.findByEmail(email);
		if (admin == null || !PasswordUtil.matches(password, admin.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid admin email or password.");
		}
		return admin;
	}

	public void sendPasswordReset(String email) {
		AdminCredential admin = adminRepository.findByEmail(email);
		if (admin == null) {
			throw new IllegalArgumentException("No admin account exists for this email.");
		}

		String token = TokenUtil.generateToken(48);
		passwordResetRepository.save(email, token, "ADMIN", LocalDateTime.now().plusMinutes(RESET_EXPIRY_MINUTES));
		String resetLink = FRONTEND_URL + "/admin/reset-password?token=" + token + "&type=admin";
		EmailUtil.sendEmail(email, "Reset admin password",
				"Hello " + admin.getDisplayName() + ",\n\nUse the link below to reset your admin password:\n"
						+ resetLink + "\n\nThis link is valid for 30 minutes.");
	}

	public void resetPassword(String token, String newPassword) {
		PasswordResetToken resetToken = passwordResetRepository.findValidToken(token);
		if (resetToken == null) {
			throw new IllegalArgumentException("Invalid reset link.");
		}
		if (resetToken.getUsedAt() != null) {
			throw new IllegalArgumentException("This reset link has already been used.");
		}
		if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("This reset link has expired.");
		}
		if (!"ADMIN".equalsIgnoreCase(resetToken.getUserType())) {
			throw new IllegalArgumentException("This reset link is not for an admin account.");
		}
		adminRepository.updatePassword(resetToken.getEmail(), PasswordUtil.hash(newPassword));
		passwordResetRepository.markUsed(resetToken.getId());
	}

	public Collection<Account> listAccounts() {
		return accountRepository.findAll();
	}

	public Account updateAccountStatus(String accountNumber, String status) throws AccountNotFoundException {
		accountService.updateStatus(accountNumber, status);
		return accountService.getAccount(accountNumber);
	}
}
