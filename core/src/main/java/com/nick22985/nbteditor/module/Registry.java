package com.nick22985.nbteditor.module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Registry<T> {
	private final RegistryKey<T> key;
	private final Map<String, RegistryEntry<T>> entries = new LinkedHashMap<>();
	private final List<Consumer<RegistryEntry<T>>> listeners = new ArrayList<>();
	private List<RegistryEntry<T>> sorted = List.of();
	private long sequence;

	public Registry(RegistryKey<T> key) {
		this.key = key;
	}

	public RegistryKey<T> key() {
		return this.key;
	}

	public synchronized T register(String owner, String id, T value, int order) {
		if (this.entries.containsKey(id)) {
			throw new IllegalStateException("Duplicate " + this.key.name() + " entry " + id + " from " + owner + ", already registered by " + this.entries.get(id).owner());
		}
		RegistryEntry<T> entry = new RegistryEntry<>(id, value, order, owner, this.sequence++);
		this.entries.put(id, entry);
		List<RegistryEntry<T>> next = new ArrayList<>(this.entries.values());
		next.sort(Comparator.comparingInt((RegistryEntry<T> e) -> e.order()).thenComparingLong(RegistryEntry::sequence));
		this.sorted = List.copyOf(next);
		for (Consumer<RegistryEntry<T>> listener : List.copyOf(this.listeners)) {
			listener.accept(entry);
		}
		return value;
	}

	public synchronized void onRegister(Consumer<RegistryEntry<T>> listener) {
		this.listeners.add(listener);
	}

	public Optional<T> get(String id) {
		RegistryEntry<T> entry = this.entries.get(id);
		return entry == null ? Optional.empty() : Optional.of(entry.value());
	}

	public boolean contains(String id) {
		return this.entries.containsKey(id);
	}

	public List<RegistryEntry<T>> entries() {
		return this.sorted;
	}

	public List<T> values() {
		List<T> out = new ArrayList<>(this.sorted.size());
		for (RegistryEntry<T> entry : this.sorted) {
			out.add(entry.value());
		}
		return out;
	}

	public List<String> ids() {
		List<String> out = new ArrayList<>(this.sorted.size());
		for (RegistryEntry<T> entry : this.sorted) {
			out.add(entry.id());
		}
		return out;
	}

	public int size() {
		return this.sorted.size();
	}
}
