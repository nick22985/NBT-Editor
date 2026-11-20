package com.nick22985.nbteditor.config;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record DoubleSettingType(double min, double max) implements SettingType<Double> {
	public static DoubleSettingType of(double min, double max) {
		return new DoubleSettingType(min, max);
	}

	@Override
	public Object toJson(Double value) {
		return value;
	}

	@Override
	public Double fromJson(Object json) {
		if (json instanceof Number number) {
			return this.normalize(number.doubleValue());
		}
		throw new IllegalArgumentException("expected number");
	}

	@Override
	public Double normalize(Double value) {
		return Math.max(this.min, Math.min(this.max, value));
	}
}
