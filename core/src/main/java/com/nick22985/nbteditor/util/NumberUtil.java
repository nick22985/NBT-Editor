package com.nick22985.nbteditor.util;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NumberUtil {
	private NumberUtil() {
	}

	public static String format(double value) {
		if (value == Math.rint(value) && Math.abs(value) < 1e15) {
			return Long.toString((long) value);
		}
		return Double.toString(value);
	}

	public static String formatFloat(float value) {
		return Float.isFinite(value) ? format(Double.parseDouble(Float.toString(value))) : Float.toString(value);
	}

	public static String formatAmount(double value) {
		if (value == Double.POSITIVE_INFINITY) {
			return "+inf";
		}
		if (value == Double.NEGATIVE_INFINITY) {
			return "-inf";
		}
		return format(value);
	}

	public static Double parseAmount(String value) {
		if (value == null) {
			return null;
		}
		switch (value.trim().toLowerCase(Locale.ROOT)) {
			case "inf":
			case "+inf":
			case "infinity":
			case "\u221e":
			case "+\u221e":
				return Double.POSITIVE_INFINITY;
			case "-inf":
			case "-infinity":
			case "-\u221e":
				return Double.NEGATIVE_INFINITY;
			case "max":
				return Double.MAX_VALUE;
			case "min":
				return -Double.MAX_VALUE;
			default:
				Double parsed = parseDouble(value);
				return parsed == null || parsed.isNaN() ? null : parsed;
		}
	}

	public static Integer parseInt(String value) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException | NullPointerException e) {
			return null;
		}
	}

	public static Double parseDouble(String value) {
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException | NullPointerException e) {
			return null;
		}
	}

	public static String roman(int value) {
		if (value <= 0 || value >= 4000) {
			return Integer.toString(value);
		}
		int[] numbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
		String[] letters = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		StringBuilder out = new StringBuilder();
		int remaining = value;
		for (int i = 0; i < numbers.length; i++) {
			while (remaining >= numbers[i]) {
				out.append(letters[i]);
				remaining -= numbers[i];
			}
		}
		return out.toString();
	}
}
