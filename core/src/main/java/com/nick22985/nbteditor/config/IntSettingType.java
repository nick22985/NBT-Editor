package com.nick22985.nbteditor.config;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record IntSettingType(int min, int max) implements SettingType<Integer> {
	public static IntSettingType of(int min, int max) {
		return new IntSettingType(min, max);
	}

	@Override
	public Object toJson(Integer value) {
		return value;
	}

	@Override
	public Integer fromJson(Object json) {
		if (json instanceof Number number) {
			return this.normalize(number.intValue());
		}
		throw new IllegalArgumentException("expected number");
	}

	@Override
	public Integer normalize(Integer value) {
		return Math.max(this.min, Math.min(this.max, value));
	}
}
