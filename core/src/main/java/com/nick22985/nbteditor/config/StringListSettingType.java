package com.nick22985.nbteditor.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StringListSettingType implements SettingType<List<String>> {
	public static final StringListSettingType INSTANCE = new StringListSettingType();

	private StringListSettingType() {
	}

	@Override
	public Object toJson(List<String> value) {
		return new ArrayList<>(value);
	}

	@Override
	public List<String> fromJson(Object json) {
		if (json instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object element : list) {
				if (element instanceof String value) {
					out.add(value);
				}
			}
			return List.copyOf(out);
		}
		throw new IllegalArgumentException("expected list");
	}
}
