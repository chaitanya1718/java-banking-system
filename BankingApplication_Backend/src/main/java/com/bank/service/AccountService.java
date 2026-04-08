package com.bank.service;

import java.math.BigDecimal;
import java.util.Collection;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.repository.AccountRepository;
import com.bank.util.PasswordUtil;

public class AccountService {

	private final AccountRepository repo;

	public AccountService(AccountRepository repo) {
		this.repo = repo;
	}

	public Account createAccount(String name, String email, BigDecimal openingBalance) throws InvalidAmountException {
		return createAccount(name, email, null, openingBalance);
	}

	public Account createAccount(String name, String email, String password, BigDecimal openingBalance)
			throws InvalidAmountException {
		validateAccountInput(name, email, openingBalance);

		if (repo.existsByEmail(email)) {
			throw new IllegalArgumentException("An account already exists for this email.");
		}

		Account acc = password == null || password.isBlank() ? new Account(name, email, openingBalance)
				: new Account(Account.nextAccountNumber(), name, email, PasswordUtil.hash(password), openingBalance, "ACTIVE", null);
		repo.save(acc);
		return acc;
	}

	public Account getAccount(String accNo) throws AccountNotFoundException {
		Account account = repo.findAccountNumber(accNo);

		if (account == null) {
			throw new AccountNotFoundException("Account not found: " + accNo);
		}

		return account;
	}

	public Account getAccountByEmail(String email) throws AccountNotFoundException {
		Account account = repo.findByEmail(email);
		if (account == null) {
			throw new AccountNotFoundException("Account not found for email: " + email);
		}
		return account;
	}

	public Account authenticate(String email, String password) throws AccountNotFoundException {
		Account account = getAccountByEmail(email);
		if (account.getPasswordHash() == null || !PasswordUtil.matches(password, account.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid email or password.");
		}
		return account;
	}

	public void updateAccount(Account account) {
		repo.update(account);
	}

	public void updatePassword(String email, String password) {
		repo.updatePassword(email, PasswordUtil.hash(password));
	}

	public void updateStatus(String accountNumber, String status) throws AccountNotFoundException {
		if (!"ACTIVE".equalsIgnoreCase(status) && !"FROZEN".equalsIgnoreCase(status)) {
			throw new IllegalArgumentException("Status must be ACTIVE or FROZEN.");
		}
		Account account = getAccount(accountNumber);
		String normalizedStatus = status.toUpperCase();
		account.setStatus(normalizedStatus);
		repo.updateStatus(accountNumber, normalizedStatus);
	}

	public Collection<Account> listAllAccounts() {
		return repo.findAll();
	}

	private void validateAccountInput(String name, String email, BigDecimal openingBalance) throws InvalidAmountException {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Name is required.");
		}
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required.");
		}
		if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidAmountException("Opening balance should not be negative.");
		}
	}
}
