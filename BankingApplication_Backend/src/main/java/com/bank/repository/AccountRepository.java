package com.bank.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bank.model.Account;

public class AccountRepository {

	public AccountRepository() {
		SchemaInitializer.ensureSchema();
		syncAccountCounter();
	}

	public void save(Account acc) {
		String query = "INSERT INTO accounts(account_number, holder_name, email, password_hash, balance, status, created_at) "
				+ "VALUES(?,?,?,?,?,?,?)";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, acc.getAccountNumber());
				pstmt.setString(2, acc.getHolderName());
				pstmt.setString(3, acc.getEmail());
				pstmt.setString(4, acc.getPasswordHash());
				pstmt.setBigDecimal(5, acc.getOpeningBalance());
				pstmt.setString(6, acc.getStatus());
				pstmt.setTimestamp(7, Timestamp.valueOf(acc.getCreatedAt()));
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to save account: " + e.getMessage(), e);
		}
	}

	public void update(Account acc) {
		String query = "UPDATE accounts SET holder_name = ?, email = ?, password_hash = ?, balance = ?, status = ? WHERE account_number = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, acc.getHolderName());
				pstmt.setString(2, acc.getEmail());
				pstmt.setString(3, acc.getPasswordHash());
				pstmt.setBigDecimal(4, acc.getOpeningBalance());
				pstmt.setString(5, acc.getStatus());
				pstmt.setString(6, acc.getAccountNumber());
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to update account: " + e.getMessage(), e);
		}
	}

	public void updatePassword(String email, String passwordHash) {
		String query = "UPDATE accounts SET password_hash = ? WHERE email = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, passwordHash);
				pstmt.setString(2, email);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to update password: " + e.getMessage(), e);
		}
	}

	public void updateStatus(String accountNumber, String status) {
		String query = "UPDATE accounts SET status = ? WHERE account_number = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, status);
				pstmt.setString(2, accountNumber);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to update account status: " + e.getMessage(), e);
		}
	}

	public Account findAccountNumber(String accountNumber) {
		String query = "SELECT account_number, holder_name, email, password_hash, balance, status, created_at "
				+ "FROM accounts WHERE account_number = ?";
		return findOne(query, accountNumber);
	}

	public Account findByEmail(String email) {
		String query = "SELECT account_number, holder_name, email, password_hash, balance, status, created_at "
				+ "FROM accounts WHERE email = ?";
		return findOne(query, email);
	}

	public boolean existsByEmail(String email) {
		return findByEmail(email) != null;
	}

	public Collection<Account> findAll() {
		String query = "SELECT account_number, holder_name, email, password_hash, balance, status, created_at "
				+ "FROM accounts ORDER BY created_at DESC, account_number";
		List<Account> accounts = new ArrayList<>();

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					accounts.add(mapAccount(rs));
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to load accounts: " + e.getMessage(), e);
		}
		return accounts;
	}

	private Account findOne(String query, String value) {
		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, value);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						return mapAccount(rs);
					}
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to load account: " + e.getMessage(), e);
		}
		return null;
	}

	private void syncAccountCounter() {
		String query = "SELECT MAX(CAST(account_number AS UNSIGNED)) AS max_account_number FROM accounts";

		try (Connection con = DBConnection.getConnection()) {
			if (con == null) {
				return;
			}
			try (PreparedStatement pstmt = con.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					long maxValue = rs.getLong("max_account_number");
					if (!rs.wasNull()) {
						Account.ensureNextAccountNumber(maxValue + 1);
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Unable to sync account counter: " + e.getMessage());
		}
	}

	private void requireConnection(Connection con) {
		if (con == null) {
			throw new IllegalStateException("Database connection is not available.");
		}
	}

	private Account mapAccount(ResultSet rs) throws SQLException {
		String accountNumber = rs.getString("account_number");
		String holderName = rs.getString("holder_name");
		String email = rs.getString("email");
		String passwordHash = rs.getString("password_hash");
		BigDecimal balance = rs.getBigDecimal("balance");
		String status = rs.getString("status");
		Timestamp createdAt = rs.getTimestamp("created_at");
		return new Account(accountNumber, holderName, email, passwordHash, balance, status,
				createdAt == null ? null : createdAt.toLocalDateTime());
	}
}
