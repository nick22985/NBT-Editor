package com.nick22985.nbteditor.data;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record FieldValue<T>(T value, boolean present, String error) {
	private static final FieldValue<?> ABSENT = new FieldValue<>(null, false, null);

	@SuppressWarnings("unchecked")
	public static <T> FieldValue<T> absent() {
		return (FieldValue<T>) ABSENT;
	}

	public static <T> FieldValue<T> of(T value) {
		return new FieldValue<>(value, true, null);
	}

	public static <T> FieldValue<T> invalid(String error) {
		return new FieldValue<>(null, true, error);
	}

	public boolean ok() {
		return error == null;
	}

	public T orElse(T fallback) {
		return value != null ? value : fallback;
	}
}
