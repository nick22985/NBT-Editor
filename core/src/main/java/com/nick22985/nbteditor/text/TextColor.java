package com.nick22985.nbteditor.text;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextColor {
	private final int rgb;
	private final Formatting named;

	private TextColor(int rgb, Formatting named) {
		this.rgb = rgb & 0xFFFFFF;
		this.named = named;
	}

	public static TextColor of(Formatting formatting) {
		if (!formatting.isColor()) {
			throw new IllegalArgumentException(formatting + " is not a colour");
		}
		return new TextColor(formatting.rgb(), formatting);
	}

	public static TextColor rgb(int rgb) {
		for (Formatting formatting : Formatting.values()) {
			if (formatting.isColor() && formatting.rgb() == (rgb & 0xFFFFFF)) {
				return new TextColor(rgb, formatting);
			}
		}
		return new TextColor(rgb, null);
	}

	public static TextColor exactRgb(int rgb) {
		return new TextColor(rgb, null);
	}

	public static TextColor parse(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		if (value.startsWith("#")) {
			try {
				return new TextColor(Integer.parseInt(value.substring(1), 16), null);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		Formatting formatting = Formatting.byName(value.toLowerCase(Locale.ROOT));
		return formatting != null && formatting.isColor() ? of(formatting) : null;
	}

	public int rgb() {
		return rgb;
	}

	public Formatting named() {
		return named;
	}

	public String serialize() {
		return named != null ? named.serializedName() : String.format("#%06X", rgb);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TextColor && ((TextColor) o).rgb == rgb && ((TextColor) o).named == named;
	}

	@Override
	public int hashCode() {
		return rgb * 31 + (named == null ? 0 : named.hashCode());
	}

	@Override
	public String toString() {
		return serialize();
	}
}
