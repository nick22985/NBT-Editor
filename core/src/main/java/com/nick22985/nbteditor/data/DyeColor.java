package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.util.ColorUtil;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum DyeColor {
	WHITE(0xFFFFFF),
	ORANGE(0xFF681F),
	MAGENTA(0xFF00FF),
	LIGHT_BLUE(0x9AC0CD),
	YELLOW(0xFFFF00),
	LIME(0xBFFF00),
	PINK(0xFF69B4),
	GRAY(0x808080),
	LIGHT_GRAY(0xD3D3D3),
	CYAN(0x00FFFF),
	PURPLE(0xA020F0),
	BLUE(0x0000FF),
	BROWN(0x8B4513),
	GREEN(0x00FF00),
	RED(0xFF0000),
	BLACK(0x000000);

	private final int textColor;

	DyeColor(int textColor) {
		this.textColor = textColor;
	}

	public static DyeColor of(String id) {
		if (id == null) {
			return null;
		}
		for (DyeColor color : values()) {
			if (color.id().equals(id)) {
				return color;
			}
		}
		return null;
	}

	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public int diffuse() {
		return ColorUtil.DYE_COLORS[this.ordinal()];
	}

	public int textColor() {
		return this.textColor;
	}

	public String translationKey() {
		return "color.minecraft." + this.id();
	}
}
