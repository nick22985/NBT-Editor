package com.nick22985.nbteditor.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StringMapSettingType implements SettingType<Map<String, String>> {
	public static final StringMapSettingType INSTANCE = new StringMapSettingType();

	private StringMapSettingType() {
	}

	@Override
	public Object toJson(Map<String, String> value) {
		return new LinkedHashMap<>(value);
	}

	@Override
	public Map<String, String> fromJson(Object json) {
		if (json instanceof Map<?, ?> map) {
			Map<String, String> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
					out.put(key, value);
				}
			}
			return Collections.unmodifiableMap(out);
		}
		throw new IllegalArgumentException("expected object");
	}
}
