package com.nick22985.nbteditor.config;

import com.nick22985.nbteditor.text.Text;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Setting<T> {
	private final ConfigSection section;
	private final String key;
	private final SettingType<T> type;
	private final T defaultValue;
	private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
	private String category = "general";
	private boolean hidden;
	private boolean requiresRestart;
	private T value;

	Setting(ConfigSection section, String key, SettingType<T> type, T defaultValue) {
		this.section = section;
		this.key = key;
		this.type = type;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String key() {
		return this.key;
	}

	public ConfigSection section() {
		return this.section;
	}

	public SettingType<T> type() {
		return this.type;
	}

	public T defaultValue() {
		return this.defaultValue;
	}

	public T get() {
		return this.value;
	}

	public void set(T value) {
		T normalized = this.type.normalize(value);
		if (Objects.equals(normalized, this.value)) {
			return;
		}
		this.value = normalized;
		this.section.changed(this);
		for (Consumer<T> listener : this.listeners) {
			listener.accept(normalized);
		}
	}

	public void reset() {
		this.set(this.defaultValue);
	}

	public boolean isDefault() {
		return Objects.equals(this.value, this.defaultValue);
	}

	public Setting<T> onChange(Consumer<T> listener) {
		this.listeners.add(listener);
		return this;
	}

	public Setting<T> category(String category) {
		this.category = category;
		return this;
	}

	public String category() {
		return this.category;
	}

	public Setting<T> hidden() {
		this.hidden = true;
		return this;
	}

	public boolean isHidden() {
		return this.hidden;
	}

	public Setting<T> requiresRestart() {
		this.requiresRestart = true;
		return this;
	}

	public boolean needsRestart() {
		return this.requiresRestart;
	}

	public String translationKey() {
		return "nbtedit." + this.section.id() + ".config." + this.key;
	}

	public Text label() {
		return Text.translatable(this.translationKey());
	}

	public Text description() {
		return Text.translatable(this.translationKey() + ".desc");
	}

	Object toJson() {
		return this.type.toJson(this.value);
	}

	void loadJson(Object json) {
		this.value = this.type.fromJson(json);
	}
}
