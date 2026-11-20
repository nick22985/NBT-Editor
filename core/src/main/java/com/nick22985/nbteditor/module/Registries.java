package com.nick22985.nbteditor.module;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Registries {
	private final Map<String, Registry<?>> registries = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <T> Registry<T> get(RegistryKey<T> key) {
		Registry<?> registry = this.registries.computeIfAbsent(key.name(), name -> new Registry<>(key));
		if (registry.key().type() != key.type()) {
			throw new IllegalStateException("Registry " + key.name() + " holds " + registry.key().type().getName() + ", not " + key.type().getName());
		}
		return (Registry<T>) registry;
	}

	public Collection<Registry<?>> all() {
		return this.registries.values();
	}
}
