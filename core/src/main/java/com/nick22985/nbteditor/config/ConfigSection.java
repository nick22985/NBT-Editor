package com.nick22985.nbteditor.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ConfigSection {
	private final ConfigStore store;
	private final String id;
	private final Map<String, Setting<?>> settings = new LinkedHashMap<>();
	private final Map<String, Object> raw;

	ConfigSection(ConfigStore store, String id, Map<String, Object> raw) {
		this.store = store;
		this.id = id;
		this.raw = raw;
	}

	public String id() {
		return this.id;
	}

	public <T> Setting<T> add(String key, SettingType<T> type, T defaultValue) {
		if (this.settings.containsKey(key)) {
			throw new IllegalStateException("duplicate setting " + this.id + "." + key);
		}
		Setting<T> setting = new Setting<>(this, key, type, defaultValue);
		Object stored = this.raw.get(key);
		if (stored != null) {
			try {
				setting.loadJson(stored);
			} catch (IllegalArgumentException e) {
				this.store.log().warn("Ignoring bad config value " + this.id + "." + key + ": " + e.getMessage());
			}
		}
		this.settings.put(key, setting);
		return setting;
	}

	public Setting<Boolean> bool(String key, boolean defaultValue) {
		return this.add(key, BoolSettingType.INSTANCE, defaultValue);
	}

	public Setting<Integer> integer(String key, int defaultValue, int min, int max) {
		return this.add(key, IntSettingType.of(min, max), defaultValue);
	}

	public Setting<Double> decimal(String key, double defaultValue, double min, double max) {
		return this.add(key, DoubleSettingType.of(min, max), defaultValue);
	}

	public Setting<String> string(String key, String defaultValue) {
		return this.add(key, StringSettingType.INSTANCE, defaultValue);
	}

	public Setting<Integer> color(String key, int defaultValue) {
		return this.add(key, ColorSettingType.INSTANCE, defaultValue);
	}

	public <E extends Enum<E>> Setting<E> enumeration(String key, E defaultValue) {
		return this.add(key, EnumSettingType.of(defaultValue.getDeclaringClass()), defaultValue);
	}

	public Setting<List<String>> stringList(String key, List<String> defaultValue) {
		return this.add(key, StringListSettingType.INSTANCE, List.copyOf(defaultValue));
	}

	public Setting<Map<String, String>> stringMap(String key, Map<String, String> defaultValue) {
		return this.add(key, StringMapSettingType.INSTANCE, Map.copyOf(defaultValue));
	}

	public Collection<Setting<?>> settings() {
		return Collections.unmodifiableCollection(this.settings.values());
	}

	public Setting<?> setting(String key) {
		return this.settings.get(key);
	}

	void changed(Setting<?> setting) {
		this.raw.put(setting.key(), setting.toJson());
		this.store.markDirty();
	}

	void writeTo(Map<String, Object> out) {
		out.putAll(this.raw);
		for (Setting<?> setting : this.settings.values()) {
			out.put(setting.key(), setting.toJson());
		}
	}
}
