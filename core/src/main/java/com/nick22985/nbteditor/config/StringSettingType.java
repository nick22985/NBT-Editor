package com.nick22985.nbteditor.config;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StringSettingType implements SettingType<String> {
	public static final StringSettingType INSTANCE = new StringSettingType();

	private StringSettingType() {
	}

	@Override
	public Object toJson(String value) {
		return value;
	}

	@Override
	public String fromJson(Object json) {
		if (json instanceof String value) {
			return value;
		}
		throw new IllegalArgumentException("expected string");
	}
}
