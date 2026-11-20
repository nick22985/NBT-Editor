package com.nick22985.nbteditor.doc;

import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Selection {
	private final LinkedHashSet<NbtPath> paths = new LinkedHashSet<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
	private NbtPath primary;
	private NbtPath anchor;

	public NbtPath primary() {
		return primary;
	}

	public NbtPath anchor() {
		return anchor;
	}

	public Set<NbtPath> paths() {
		return Collections.unmodifiableSet(paths);
	}

	public int size() {
		return paths.size();
	}

	public boolean isEmpty() {
		return paths.isEmpty();
	}

	public boolean contains(NbtPath path) {
		return paths.contains(path);
	}

	public void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public void removeListener(Runnable listener) {
		listeners.remove(listener);
	}

	public void select(NbtPath path) {
		paths.clear();
		if (path != null) {
			paths.add(path);
		}
		primary = path;
		anchor = path;
		fire();
	}

	public void toggle(NbtPath path) {
		if (!paths.remove(path)) {
			paths.add(path);
			primary = path;
		} else if (path.equals(primary)) {
			primary = paths.isEmpty() ? null : last();
		}
		anchor = path;
		fire();
	}

	public void extendTo(NbtPath path, List<NbtPath> visibleOrder) {
		if (anchor == null) {
			select(path);
			return;
		}
		int from = visibleOrder.indexOf(anchor);
		int to = visibleOrder.indexOf(path);
		if (from < 0 || to < 0) {
			select(path);
			return;
		}
		paths.clear();
		for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
			paths.add(visibleOrder.get(i));
		}
		primary = path;
		fire();
	}

	public void selectAll(List<NbtPath> visibleOrder) {
		paths.clear();
		paths.addAll(visibleOrder);
		if (primary == null && !visibleOrder.isEmpty()) {
			primary = visibleOrder.get(0);
			anchor = primary;
		}
		fire();
	}

	public void clear() {
		if (paths.isEmpty() && primary == null) {
			return;
		}
		paths.clear();
		primary = null;
		anchor = null;
		fire();
	}

	public List<NbtPath> roots() {
		List<NbtPath> sorted = new ArrayList<>(paths);
		Collections.sort(sorted);
		List<NbtPath> out = new ArrayList<>();
		for (NbtPath path : sorted) {
			boolean covered = false;
			for (NbtPath kept : out) {
				if (path.startsWith(kept)) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				out.add(path);
			}
		}
		return out;
	}

	public List<NbtPath> removalOrder() {
		List<NbtPath> out = roots();
		Collections.reverse(out);
		return out;
	}

	void retainValid(Tag root) {
		boolean changed = paths.removeIf(path -> path.get(root) == null);
		if (primary != null && primary.get(root) == null) {
			primary = paths.isEmpty() ? null : last();
			changed = true;
		}
		if (anchor != null && anchor.get(root) == null) {
			anchor = primary;
		}
		if (changed) {
			fire();
		}
	}

	private NbtPath last() {
		NbtPath result = null;
		for (NbtPath path : paths) {
			result = path;
		}
		return result;
	}

	private void fire() {
		for (Runnable listener : listeners) {
			listener.run();
		}
	}
}
