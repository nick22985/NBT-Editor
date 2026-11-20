package com.nick22985.nbteditor.config;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface SettingType<T> {
	Object toJson(T value);

	T fromJson(Object json);

	default T normalize(T value) {
		return value;
	}
}
