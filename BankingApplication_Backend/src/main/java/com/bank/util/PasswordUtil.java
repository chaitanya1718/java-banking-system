package com.bank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PasswordUtil {

	private PasswordUtil() {
	}

	public static String hash(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			throw new IllegalArgumentException("Password is required.");
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Unable to hash password.", e);
		}
	}

	public static boolean matches(String plainText, String passwordHash) {
		return hash(plainText).equals(passwordHash);
	}
}
