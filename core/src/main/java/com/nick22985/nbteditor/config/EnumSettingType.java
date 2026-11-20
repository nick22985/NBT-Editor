package com.nick22985.nbteditor.config;

import java.util.List;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EnumSettingType<E extends Enum<E>> implements SettingType<E> {
	private final Class<E> type;

	private EnumSettingType(Class<E> type) {
		this.type = type;
	}

	public static <E extends Enum<E>> EnumSettingType<E> of(Class<E> type) {
		return new EnumSettingType<>(type);
	}

	public List<E> values() {
		return List.of(this.type.getEnumConstants());
	}

	@Override
	public Object toJson(E value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public E fromJson(Object json) {
		if (json instanceof String value) {
			for (E constant : this.type.getEnumConstants()) {
				if (constant.name().equalsIgnoreCase(value)) {
					return constant;
				}
			}
		}
		throw new IllegalArgumentException("unknown " + this.type.getSimpleName() + " " + json);
	}
}
