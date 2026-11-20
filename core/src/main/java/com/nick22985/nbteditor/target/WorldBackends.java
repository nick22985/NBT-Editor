package com.nick22985.nbteditor.target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class WorldBackends {
	private final List<WorldBackend> backends = new CopyOnWriteArrayList<>();

	public void add(WorldBackend backend) {
		this.backends.add(backend);
		List<WorldBackend> sorted = new ArrayList<>(this.backends);
		sorted.sort(Comparator.comparingInt(WorldBackend::priority).reversed());
		this.backends.clear();
		this.backends.addAll(sorted);
	}

	public List<WorldBackend> all() {
		return List.copyOf(this.backends);
	}

	public Optional<WorldBackend> best(SubjectKind kind, TargetCapability capability) {
		for (WorldBackend backend : this.backends) {
			if (backend.available() && backend.supports(kind, capability)) {
				return Optional.of(backend);
			}
		}
		return Optional.empty();
	}

	public boolean any() {
		for (WorldBackend backend : this.backends) {
			if (backend.available()) {
				return true;
			}
		}
		return false;
	}
}
