package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SchemaInitializer {

	private static boolean initialized;

	private SchemaInitializer() {
	}

	public static synchronized void ensureSchema() {
		if (initialized) {
			return;
		}

		try (Connection con = DBConnection.getConnection()) {
			if (con == null) {
				return;
			}

			run(con, "CREATE TABLE IF NOT EXISTS accounts ("
					+ "account_number VARCHAR(50) NOT NULL PRIMARY KEY,"
					+ "holder_name VARCHAR(255) NOT NULL,"
					+ "email VARCHAR(255) NOT NULL,"
					+ "balance DECIMAL(15,2) NOT NULL)");
			addColumnIfMissing(con, "accounts", "password_hash", "ALTER TABLE accounts ADD COLUMN password_hash VARCHAR(255) NULL");
			addColumnIfMissing(con, "accounts", "status", "ALTER TABLE accounts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
			addColumnIfMissing(con, "accounts", "created_at", "ALTER TABLE accounts ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

			run(con, "CREATE TABLE IF NOT EXISTS transactions ("
					+ "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
					+ "type VARCHAR(50) NOT NULL,"
					+ "account_number VARCHAR(50) NOT NULL,"
					+ "amount DECIMAL(15,2) NOT NULL,"
					+ "target_account VARCHAR(50) NULL,"
					+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

			run(con, "CREATE TABLE IF NOT EXISTS email_otps ("
					+ "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
					+ "email VARCHAR(255) NOT NULL,"
					+ "otp_code VARCHAR(10) NOT NULL,"
					+ "purpose VARCHAR(50) NOT NULL,"
					+ "expires_at TIMESTAMP NOT NULL,"
					+ "verified_at TIMESTAMP NULL,"
					+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

			run(con, "CREATE TABLE IF NOT EXISTS password_reset_tokens ("
					+ "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
					+ "email VARCHAR(255) NOT NULL,"
					+ "token VARCHAR(255) NOT NULL,"
					+ "user_type VARCHAR(20) NOT NULL,"
					+ "expires_at TIMESTAMP NOT NULL,"
					+ "used_at TIMESTAMP NULL,"
					+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

			run(con, "CREATE TABLE IF NOT EXISTS admin_credentials ("
					+ "email VARCHAR(255) NOT NULL PRIMARY KEY,"
					+ "password_hash VARCHAR(255) NOT NULL,"
					+ "display_name VARCHAR(255) NOT NULL,"
					+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to initialize schema: " + e.getMessage(), e);
		}

		initialized = true;
	}

	private static void addColumnIfMissing(Connection con, String tableName, String columnName, String alterSql)
			throws SQLException {
		String sql = "SELECT COUNT(*) AS column_count FROM information_schema.columns "
				+ "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, tableName);
			pstmt.setString(2, columnName);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next() && rs.getInt("column_count") == 0) {
					run(con, alterSql);
				}
			}
		}
	}

	private static void run(Connection con, String sql) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.execute();
		}
	}
}
