package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionEntry {

	private long id;
	private String type;
	private String accountNumber;
	private BigDecimal amount;
	private String targetAccount;
	private LocalDateTime createdAt;

	public TransactionEntry(long id, String type, String accountNumber, BigDecimal amount, String targetAccount,
			LocalDateTime createdAt) {
		this.id = id;
		this.type = type;
		this.accountNumber = accountNumber;
		this.amount = amount;
		this.targetAccount = targetAccount;
		this.createdAt = createdAt;
	}

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getTargetAccount() {
		return targetAccount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
