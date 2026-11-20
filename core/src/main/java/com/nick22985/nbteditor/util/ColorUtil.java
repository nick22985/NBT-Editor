package com.nick22985.nbteditor.util;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ColorUtil {
	public static final int[] FORMATTING_COLORS = {
		0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
		0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
	};
	public static final int[] DYE_COLORS = {
		0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
		0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
	};
	public static final int LEATHER_COLOR = 0xA06540;

	private ColorUtil() {
	}

	public static int hsvToRgb(float hue, float saturation, float value) {
		float h = (hue - (float) Math.floor(hue)) * 6.0F;
		int sector = (int) h;
		float f = h - sector;
		float p = value * (1.0F - saturation);
		float q = value * (1.0F - f * saturation);
		float t = value * (1.0F - (1.0F - f) * saturation);
		float r;
		float g;
		float b;
		switch (sector) {
			case 0:
				r = value;
				g = t;
				b = p;
				break;
			case 1:
				r = q;
				g = value;
				b = p;
				break;
			case 2:
				r = p;
				g = value;
				b = t;
				break;
			case 3:
				r = p;
				g = q;
				b = value;
				break;
			case 4:
				r = t;
				g = p;
				b = value;
				break;
			default:
				r = value;
				g = p;
				b = q;
				break;
		}
		return rgb(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
	}

	public static float[] rgbToHsv(int rgb) {
		float r = red(rgb) / 255.0F;
		float g = green(rgb) / 255.0F;
		float b = blue(rgb) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue;
		if (delta == 0) {
			hue = 0;
		} else if (max == r) {
			hue = ((g - b) / delta) / 6.0F;
		} else if (max == g) {
			hue = ((b - r) / delta + 2) / 6.0F;
		} else {
			hue = ((r - g) / delta + 4) / 6.0F;
		}
		if (hue < 0) {
			hue += 1;
		}
		float saturation = max == 0 ? 0 : delta / max;
		return new float[] {hue, saturation, max};
	}

	public static int rgb(int red, int green, int blue) {
		return (clampChannel(red) << 16) | (clampChannel(green) << 8) | clampChannel(blue);
	}

	public static int red(int rgb) {
		return rgb >> 16 & 0xFF;
	}

	public static int green(int rgb) {
		return rgb >> 8 & 0xFF;
	}

	public static int blue(int rgb) {
		return rgb & 0xFF;
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | rgb & 0xFFFFFF;
	}

	public static int lerp(int from, int to, float progress) {
		return rgb(
			Math.round(red(from) + (red(to) - red(from)) * progress),
			Math.round(green(from) + (green(to) - green(from)) * progress),
			Math.round(blue(from) + (blue(to) - blue(from)) * progress));
	}

	public static String toHex(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}

	public static Integer parseHex(String value) {
		if (value == null) {
			return null;
		}
		String hex = value.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		} else if (hex.startsWith("0x") || hex.startsWith("0X")) {
			hex = hex.substring(2);
		}
		if (hex.length() == 3) {
			hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
		}
		if (hex.length() != 6) {
			return null;
		}
		try {
			return Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static float luminance(int rgb) {
		return (0.2126F * red(rgb) + 0.7152F * green(rgb) + 0.0722F * blue(rgb)) / 255.0F;
	}

	private static int clampChannel(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
