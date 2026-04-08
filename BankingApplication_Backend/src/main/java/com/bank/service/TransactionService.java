package com.bank.service;

import java.math.BigDecimal;
import java.util.List;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.TransactionEntry;
import com.bank.repository.TransactionRepository;
import com.bank.util.FileReportUtil;

public class TransactionService {

	private final AccountService accountService;
	private final TransactionRepository txRepo;
	private final AlertService alertservice;

	public TransactionService(AccountService accountService, TransactionRepository txRepo, AlertService alertservice) {
		this.accountService = accountService;
		this.txRepo = txRepo;
		this.alertservice = alertservice;
	}

	public void deposite(String accNo, BigDecimal amount)
			throws AccountNotFoundException, InvalidAmountException {
		validateAmount(amount);

		Account account = accountService.getAccount(accNo);
		ensureAccountIsActive(account);
		account.credit(amount);
		accountService.updateAccount(account);
		txRepo.logTransaction("DEPOSIT", accNo, amount.doubleValue(), null);
		FileReportUtil.writeLine("DEPOSIT | Acc: " + accNo + " | Amount: " + amount);
		alertservice.checkBalance(account);
	}

	public void withdraw(String accNo, BigDecimal amount)
			throws AccountNotFoundException, InvalidAmountException, InsufficientBalanceException {
		validateAmount(amount);

		Account account = accountService.getAccount(accNo);
		ensureAccountIsActive(account);
		if (account.getOpeningBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient balance.");
		}

		account.debit(amount);
		accountService.updateAccount(account);
		txRepo.logTransaction("WITHDRAW", accNo, amount.doubleValue(), null);
		FileReportUtil.writeLine("WITHDRAW | Acc: " + accNo + " | Amount: " + amount);
		alertservice.checkBalance(account);
	}

	public void transfer(String fromAcc, String toAcc, BigDecimal amount)
			throws InvalidAmountException, AccountNotFoundException, InsufficientBalanceException {
		validateAmount(amount);

		Account sender = accountService.getAccount(fromAcc);
		Account receiver = accountService.getAccount(toAcc);
		ensureAccountIsActive(sender);
		ensureAccountIsActive(receiver);

		if (sender.getOpeningBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient balance.");
		}

		sender.debit(amount);
		accountService.updateAccount(sender);
		alertservice.checkBalance(sender);

		receiver.credit(amount);
		accountService.updateAccount(receiver);
		alertservice.checkBalance(receiver);

		txRepo.logTransaction("TRANSFER", fromAcc, amount.doubleValue(), toAcc);
		txRepo.logTransaction("TRANSFER_IN", toAcc, amount.doubleValue(), fromAcc);
		FileReportUtil.writeLine("TRANSFER | Acc: " + fromAcc + " | Amount: " + amount + " | to " + toAcc);
	}

	public List<TransactionEntry> getStatement(String accountNumber) throws AccountNotFoundException {
		accountService.getAccount(accountNumber);
		return txRepo.findByAccountNumber(accountNumber);
	}

	private void validateAmount(BigDecimal amount) throws InvalidAmountException {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException("Amount must be positive.");
		}
	}

	private void ensureAccountIsActive(Account account) {
		if (account.isFrozen()) {
			throw new IllegalStateException("This account is frozen. Transactions are disabled.");
		}
	}
}
