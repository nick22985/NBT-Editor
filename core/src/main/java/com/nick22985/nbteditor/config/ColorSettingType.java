package com.nick22985.nbteditor.config;

import com.nick22985.nbteditor.util.ColorUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ColorSettingType implements SettingType<Integer> {
	public static final ColorSettingType INSTANCE = new ColorSettingType();

	private ColorSettingType() {
	}

	@Override
	public Object toJson(Integer value) {
		return ColorUtil.toHex(value);
	}

	@Override
	public Integer fromJson(Object json) {
		if (json instanceof String value) {
			Integer parsed = ColorUtil.parseHex(value);
			if (parsed != null) {
				return parsed;
			}
		}
		if (json instanceof Number number) {
			return number.intValue() & 0xFFFFFF;
		}
		throw new IllegalArgumentException("expected colour");
	}
}
