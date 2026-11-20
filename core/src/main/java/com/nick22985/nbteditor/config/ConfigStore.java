package com.nick22985.nbteditor.config;

import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.util.JsonUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ConfigStore {
	private static final int VERSION = 1;

	private final Path file;
	private final Log log;
	private final Map<String, ConfigSection> sections = new LinkedHashMap<>();
	private final Map<String, Object> rawSections = new LinkedHashMap<>();
	private final Map<String, Boolean> modules = new LinkedHashMap<>();
	private boolean dirty;

	public ConfigStore(Path file, Log log) {
		this.file = file;
		this.log = log;
	}

	Log log() {
		return this.log;
	}

	public Path file() {
		return this.file;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		this.rawSections.clear();
		this.modules.clear();
		if (!Files.exists(this.file)) {
			return;
		}
		try {
			Map<String, Object> root = JsonUtil.parseObject(Files.readString(this.file, StandardCharsets.UTF_8));
			if (root.get("modules") instanceof Map<?, ?> moduleMap) {
				for (Map.Entry<?, ?> entry : moduleMap.entrySet()) {
					if (entry.getKey() instanceof String id && entry.getValue() instanceof Boolean enabled) {
						this.modules.put(id, enabled);
					}
				}
			}
			if (root.get("settings") instanceof Map<?, ?> settingMap) {
				for (Map.Entry<?, ?> entry : settingMap.entrySet()) {
					if (entry.getKey() instanceof String id && entry.getValue() instanceof Map<?, ?> values) {
						this.rawSections.put(id, new LinkedHashMap<>((Map<String, Object>) values));
					}
				}
			}
		} catch (IOException | JsonException | ClassCastException e) {
			this.log.error("Could not read config " + this.file + ", using defaults", e);
			try {
				Files.copy(this.file, this.file.resolveSibling(this.file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				this.log.warn("Could not back up broken config");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public ConfigSection section(String id) {
		return this.sections.computeIfAbsent(id, key -> {
			Object raw = this.rawSections.computeIfAbsent(key, ignored -> new LinkedHashMap<String, Object>());
			return new ConfigSection(this, key, (Map<String, Object>) raw);
		});
	}

	public Collection<ConfigSection> sections() {
		return Collections.unmodifiableCollection(this.sections.values());
	}

	public boolean moduleEnabled(String id, boolean defaultValue) {
		return this.modules.getOrDefault(id, defaultValue);
	}

	public void setModuleEnabled(String id, boolean enabled) {
		this.modules.put(id, enabled);
		this.markDirty();
	}

	public boolean hasModuleSetting(String id) {
		return this.modules.containsKey(id);
	}

	void markDirty() {
		this.dirty = true;
	}

	public boolean dirty() {
		return this.dirty;
	}

	public void saveIfDirty() {
		if (this.dirty) {
			this.save();
		}
	}

	public void save() {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("version", VERSION);
		root.put("modules", new LinkedHashMap<>(this.modules));
		Map<String, Object> settings = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : this.rawSections.entrySet()) {
			ConfigSection section = this.sections.get(entry.getKey());
			Map<String, Object> values = new LinkedHashMap<>();
			if (section != null) {
				section.writeTo(values);
			} else if (entry.getValue() instanceof Map<?, ?> raw) {
				for (Map.Entry<?, ?> value : raw.entrySet()) {
					values.put(String.valueOf(value.getKey()), value.getValue());
				}
			}
			if (!values.isEmpty()) {
				settings.put(entry.getKey(), values);
			}
		}
		root.put("settings", settings);
		try {
			Files.createDirectories(this.file.getParent());
			Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
			Files.writeString(temp, JsonUtil.pretty(root), StandardCharsets.UTF_8);
			Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			this.dirty = false;
		} catch (IOException e) {
			this.log.error("Could not save config " + this.file, e);
		}
	}
}
