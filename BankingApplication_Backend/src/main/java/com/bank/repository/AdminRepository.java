package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.bank.model.AdminCredential;
import com.bank.util.EnvUtil;
import com.bank.util.PasswordUtil;

public class AdminRepository {

	private static final String DEFAULT_ADMIN_EMAIL = EnvUtil.getOrDefault("BANK_ADMIN_EMAIL", "chaitanyag1718@gmail.com");
	private static final String DEFAULT_ADMIN_PASSWORD = EnvUtil.getOrDefault("BANK_ADMIN_PASSWORD", "Admin@bank123");
	private static final String DEFAULT_ADMIN_NAME = EnvUtil.getOrDefault("BANK_ADMIN_NAME", "Bank Manager");

	public AdminRepository() {
		SchemaInitializer.ensureSchema();
		ensureAdminExists();
	}

	public AdminCredential findByEmail(String email) {
		String query = "SELECT email, password_hash, display_name, updated_at FROM admin_credentials WHERE email = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, email);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						return new AdminCredential(rs.getString("email"), rs.getString("password_hash"),
								rs.getString("display_name"),
								rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
					}
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to load admin credentials: " + e.getMessage(), e);
		}
		return null;
	}

	public void updatePassword(String email, String passwordHash) {
		String query = "UPDATE admin_credentials SET password_hash = ?, updated_at = ? WHERE email = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, passwordHash);
				pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				pstmt.setString(3, email);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to update admin password: " + e.getMessage(), e);
		}
	}

	private void ensureAdminExists() {
		String select = "SELECT COUNT(*) AS admin_count FROM admin_credentials WHERE email = ?";
		String insert = "INSERT INTO admin_credentials(email, password_hash, display_name) VALUES(?,?,?)";

		try (Connection con = DBConnection.getConnection()) {
			if (con == null) {
				return;
			}
			try (PreparedStatement selectStmt = con.prepareStatement(select)) {
				selectStmt.setString(1, DEFAULT_ADMIN_EMAIL);
				try (ResultSet rs = selectStmt.executeQuery()) {
					if (rs.next() && rs.getInt("admin_count") > 0) {
						return;
					}
				}
			}

			try (PreparedStatement insertStmt = con.prepareStatement(insert)) {
				insertStmt.setString(1, DEFAULT_ADMIN_EMAIL);
				insertStmt.setString(2, PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD));
				insertStmt.setString(3, DEFAULT_ADMIN_NAME);
				insertStmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to ensure admin credentials: " + e.getMessage(), e);
		}
	}

	private void requireConnection(Connection con) {
		if (con == null) {
			throw new IllegalStateException("Database connection is not available.");
		}
	}
}
