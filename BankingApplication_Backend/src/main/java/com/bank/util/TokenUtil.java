package com.bank.util;

import java.security.SecureRandom;

public final class TokenUtil {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

	private TokenUtil() {
	}

	public static String generateOtp() {
		int value = 100000 + RANDOM.nextInt(900000);
		return String.valueOf(value);
	}

	public static String generateToken(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int index = 0; index < length; index++) {
			builder.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
		}
		return builder.toString();
	}
}
