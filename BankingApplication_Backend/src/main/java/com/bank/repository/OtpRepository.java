package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.bank.model.OtpVerification;

public class OtpRepository {

	public OtpRepository() {
		SchemaInitializer.ensureSchema();
	}

	public void save(String email, String otpCode, String purpose, LocalDateTime expiresAt) {
		String query = "INSERT INTO email_otps(email, otp_code, purpose, expires_at) VALUES(?,?,?,?)";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, email);
				pstmt.setString(2, otpCode);
				pstmt.setString(3, purpose);
				pstmt.setTimestamp(4, Timestamp.valueOf(expiresAt));
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to save OTP verification: " + e.getMessage(), e);
		}
	}

	public OtpVerification findLatest(String email, String purpose) {
		String query = "SELECT id, email, otp_code, purpose, expires_at, verified_at, created_at "
				+ "FROM email_otps WHERE email = ? AND purpose = ? ORDER BY created_at DESC, id DESC LIMIT 1";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, email);
				pstmt.setString(2, purpose);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						return mapOtp(rs);
					}
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to load OTP verification: " + e.getMessage(), e);
		}
		return null;
	}

	public void markVerified(long id) {
		String query = "UPDATE email_otps SET verified_at = ? WHERE id = ?";

		try (Connection con = DBConnection.getConnection()) {
			requireConnection(con);
			try (PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
				pstmt.setLong(2, id);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to update OTP verification: " + e.getMessage(), e);
		}
	}

	private OtpVerification mapOtp(ResultSet rs) throws SQLException {
		return new OtpVerification(rs.getLong("id"), rs.getString("email"), rs.getString("otp_code"),
				rs.getString("purpose"), rs.getTimestamp("expires_at").toLocalDateTime(),
				rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toLocalDateTime(),
				rs.getTimestamp("created_at").toLocalDateTime());
	}

	private void requireConnection(Connection con) {
		if (con == null) {
			throw new IllegalStateException("Database connection is not available.");
		}
	}
}
