package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class Account {
	
	private static final AtomicLong COUNTER= new AtomicLong(10000000L);
	private String accountNumber;
	private String holderName;
	private String email;
	private String passwordHash;
	private BigDecimal openingBalance;
	private String status;
	private LocalDateTime createdAt;
	
	public Account(String holderName, String email,BigDecimal openingBalance) {
		this(nextAccountNumber(), holderName, email, null, openingBalance, "ACTIVE", LocalDateTime.now());
	}

	public Account(String accountNumber, String holderName, String email, BigDecimal openingBalance) {
		this(accountNumber, holderName, email, null, openingBalance, "ACTIVE", LocalDateTime.now());
	}

	public Account(String accountNumber, String holderName, String email, String passwordHash, BigDecimal openingBalance,
			String status, LocalDateTime createdAt) {
		this.accountNumber = accountNumber;
		this.holderName = holderName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.openingBalance = openingBalance == null ? BigDecimal.ZERO : openingBalance;
		this.status = status == null || status.isBlank() ? "ACTIVE" : status;
		this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
	}

	public static void ensureNextAccountNumber(long nextValue) {
		COUNTER.updateAndGet(current -> Math.max(current, nextValue));
	}

	public static String nextAccountNumber() {
		return String.valueOf(COUNTER.getAndIncrement());
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getHolderName() {
		return holderName;
	}

	public void setHolderName(String holderName) {
		this.holderName = holderName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public BigDecimal getOpeningBalance() {
		return openingBalance;
	}

	public void setOpeningBalance(BigDecimal openingBalance) {
		this.openingBalance = openingBalance;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isFrozen() {
		return "FROZEN".equalsIgnoreCase(status);
	}

	public void credit(BigDecimal balance){
		this.openingBalance = this.openingBalance.add(balance);
	}
	
	public void debit(BigDecimal balance){
		this.openingBalance = this.openingBalance.subtract(balance);
	}
	
	
	public String toString(){
		return accountNumber+" "+holderName+" "+email+" "+openingBalance+" "+status;
	}
	
	

}
