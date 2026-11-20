package com.nick22985.nbteditor.module;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Services {
	private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

	public <T> T provide(Class<T> type, T service) {
		Object previous = this.services.putIfAbsent(type, service);
		if (previous != null) {
			throw new IllegalStateException("Service " + type.getName() + " is already provided");
		}
		return service;
	}

	public <T> Optional<T> find(Class<T> type) {
		return Optional.ofNullable(type.cast(this.services.get(type)));
	}

	public <T> T get(Class<T> type) {
		Object service = this.services.get(type);
		if (service == null) {
			throw new IllegalStateException("No service " + type.getName() + "; is the module that provides it enabled?");
		}
		return type.cast(service);
	}

	public boolean has(Class<?> type) {
		return this.services.containsKey(type);
	}
}
