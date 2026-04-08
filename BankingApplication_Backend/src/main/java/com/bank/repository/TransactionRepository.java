package com.bank.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.bank.model.TransactionEntry;

public class TransactionRepository {

	public TransactionRepository() {
		SchemaInitializer.ensureSchema();
	}

	public void logTransaction(String type, String accNo, double amount, String targetAcc) {
		String query = "INSERT INTO transactions(type, account_number, amount, target_account) VALUES(?,?,?,?)";

		try (Connection con = DBConnection.getConnection()) {
			if (con == null) {
				System.out.println("Skipping DB transaction log because no database connection is available.");
				return;
			}
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, type);
				pstmt.setString(2, accNo);
				pstmt.setDouble(3, amount);
				pstmt.setString(4, targetAcc);
				pstmt.executeUpdate();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public List<TransactionEntry> findByAccountNumber(String accountNumber) {
		String query = "SELECT id, type, account_number, amount, target_account, created_at "
				+ "FROM transactions WHERE account_number = ? ORDER BY created_at DESC, id DESC";
		List<TransactionEntry> entries = new ArrayList<>();

		try (Connection con = DBConnection.getConnection()) {
			if (con == null) {
				throw new IllegalStateException("Database connection is not available.");
			}
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, accountNumber);
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						entries.add(new TransactionEntry(rs.getLong("id"), rs.getString("type"),
								rs.getString("account_number"), rs.getBigDecimal("amount"),
								rs.getString("target_account"), rs.getTimestamp("created_at").toLocalDateTime()));
					}
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to load transactions: " + e.getMessage(), e);
		}

		return entries;
	}
}
