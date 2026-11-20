package com.nick22985.nbteditor.config;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BoolSettingType implements SettingType<Boolean> {
	public static final BoolSettingType INSTANCE = new BoolSettingType();

	private BoolSettingType() {
	}

	@Override
	public Object toJson(Boolean value) {
		return value;
	}

	@Override
	public Boolean fromJson(Object json) {
		if (json instanceof Boolean value) {
			return value;
		}
		throw new IllegalArgumentException("expected boolean");
	}
}
