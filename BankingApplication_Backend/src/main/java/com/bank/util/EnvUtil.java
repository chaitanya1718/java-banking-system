package com.bank.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnvUtil {

	private static final Map<String, String> FILE_VALUES = loadEnvFile();

	private EnvUtil() {
	}

	public static String get(String key) {
		String envValue = System.getenv(key);
		if (envValue != null && !envValue.isBlank()) {
			return envValue;
		}
		return FILE_VALUES.get(key);
	}

	public static String getOrDefault(String key, String defaultValue) {
		String value = get(key);
		return (value == null || value.isBlank()) ? defaultValue : value;
	}

	private static Map<String, String> loadEnvFile() {
		Map<String, String> values = new HashMap<>();
		Path envPath = Paths.get(".env");
		if (!Files.exists(envPath)) {
			return values;
		}

		try {
			List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}

				int separatorIndex = trimmed.indexOf('=');
				if (separatorIndex <= 0) {
					continue;
				}

				String key = trimmed.substring(0, separatorIndex).trim();
				String value = trimmed.substring(separatorIndex + 1).trim();
				values.put(key, stripQuotes(value));
			}
		} catch (IOException e) {
			System.out.println("Unable to read .env file: " + e.getMessage());
		}

		return values;
	}

	private static String stripQuotes(String value) {
		if (value.length() >= 2) {
			boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
			boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
			if (doubleQuoted || singleQuoted) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}
}
