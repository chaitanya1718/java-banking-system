package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.bank.model.PasswordResetToken;

public class PasswordResetRepository {

	public PasswordResetRepository() {
		SchemaInitializer.ensureSchema();
	}

	public void save(String email, String token, String userType, LocalDateTime expiresAt) {
		String query = "INSERT INTO password_reset_tokens(email, token, user_type, expires_at) VALUES(?,?,?,?)";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, email);
				pstmt.setString(2, token);
				pstmt.setString(3, userType);
				pstmt.setTimestamp(4, Timestamp.valueOf(expiresAt));
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to save password reset token: " + e.getMessage(), e);
		}
	}

	public PasswordResetToken findValidToken(String token) {
		String query = "SELECT id, email, token, user_type, expires_at, used_at, created_at "
				+ "FROM password_reset_tokens WHERE token = ? ORDER BY created_at DESC LIMIT 1";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, token);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						return mapToken(rs);
					}
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to load password reset token: " + e.getMessage(), e);
		}
		return null;
	}

	public void markUsed(long id) {
		String query = "UPDATE password_reset_tokens SET used_at = ? WHERE id = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
				pstmt.setLong(2, id);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to update password reset token: " + e.getMessage(), e);
		}
	}

	private PasswordResetToken mapToken(ResultSet rs) throws SQLException {
		return new PasswordResetToken(rs.getLong("id"), rs.getString("email"), rs.getString("token"),
				rs.getString("user_type"), rs.getTimestamp("expires_at").toLocalDateTime(),
				rs.getTimestamp("used_at") == null ? null : rs.getTimestamp("used_at").toLocalDateTime(),
				rs.getTimestamp("created_at").toLocalDateTime());
	}

	private void requireConnection(Connection con) {
		if (con == null) {
			throw new IllegalStateException("Database connection is not available.");
		}
	}
}
