package com.nick22985.nbteditor.hook;

import com.nick22985.nbteditor.log.Log;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Hooks {
	private final Map<HookKey<?>, List<HookListener<?>>> listeners = new ConcurrentHashMap<>();
	private final Log log;

	public Hooks(Log log) {
		this.log = log;
	}

	public <E> void on(HookKey<E> key, Consumer<E> listener) {
		this.on(key, 0, listener);
	}

	public <E> void on(HookKey<E> key, int priority, Consumer<E> listener) {
		List<HookListener<?>> list = this.listeners.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
		list.add(new HookListener<>(priority, listener));
		list.sort(Comparator.comparingInt(HookListener::priority));
	}

	public boolean has(HookKey<?> key) {
		List<HookListener<?>> list = this.listeners.get(key);
		return list != null && !list.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public <E> E fire(HookKey<E> key, E event) {
		List<HookListener<?>> list = this.listeners.get(key);
		if (list == null) {
			return event;
		}
		for (HookListener<?> listener : list) {
			try {
				((Consumer<E>) listener.listener()).accept(event);
			} catch (RuntimeException e) {
				this.log.error("Hook " + key.name() + " listener failed", e);
			}
			if (event instanceof CancellableEvent cancellable && cancellable.cancelled()) {
				break;
			}
		}
		return event;
	}
}
