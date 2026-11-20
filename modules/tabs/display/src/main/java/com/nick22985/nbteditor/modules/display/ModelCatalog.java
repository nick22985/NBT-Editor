package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.util.FolderClusterUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelCatalog {
	private final List<ModelEntry> entries;
	private final List<String> packs = new ArrayList<>();
	private final List<String> items = new ArrayList<>();
	private final Map<ModelFolder, Set<String>> children = new HashMap<>();
	private final Map<ModelEntry, String> real = new HashMap<>();
	private final Map<ModelEntry, String> haystack = new HashMap<>();

	public ModelCatalog(List<ModelEntry> entries, List<String> packOrder) {
		this.entries = this.cluster(entries);
		Set<String> found = new TreeSet<>();
		Set<String> items = new TreeSet<>();
		for (ModelEntry entry : this.entries) {
			found.add(entry.pack());
			items.add(entry.model().item());
			ModelFolder folder = ModelFolder.ALL;
			this.link(folder, entry.pack());
			folder = folder.child(entry.pack());
			if (!entry.folder().isEmpty()) {
				for (String part : entry.folder().split("/")) {
					this.link(folder, part);
					folder = folder.child(part);
				}
			}
		}
		for (String pack : packOrder) {
			if (found.remove(pack)) {
				this.packs.add(pack);
			}
		}
		this.packs.addAll(found);
		this.items.addAll(items);
	}

	private List<ModelEntry> cluster(List<ModelEntry> entries) {
		Map<String, Set<String>> folders = new HashMap<>();
		for (ModelEntry entry : entries) {
			folders.computeIfAbsent(entry.pack(), key -> new TreeSet<>()).add(entry.folder());
		}
		Map<String, Map<String, String>> paths = new HashMap<>();
		for (Map.Entry<String, Set<String>> pack : folders.entrySet()) {
			paths.put(pack.getKey(), FolderClusterUtil.paths(pack.getValue()));
		}
		List<ModelEntry> out = new ArrayList<>();
		for (ModelEntry entry : entries) {
			String display = paths.get(entry.pack()).getOrDefault(entry.folder(), entry.folder());
			ModelEntry shown = new ModelEntry(entry.model(), entry.pack(), display, entry.order());
			this.real.put(shown, entry.folder());
			this.haystack.put(shown, String.join("\n", entry.model().label(), entry.model().model(), entry.folder(), display, entry.pack()).toLowerCase(Locale.ROOT));
			out.add(shown);
		}
		return List.copyOf(out);
	}

	public String realFolder(ModelEntry entry) {
		String folder = this.real.get(entry);
		return folder == null ? entry.folder() : folder;
	}

	private void link(ModelFolder parent, String child) {
		this.children.computeIfAbsent(parent, key -> new TreeSet<>()).add(child);
	}

	public List<ModelEntry> entries() {
		return this.entries;
	}

	public List<String> packs() {
		return this.packs;
	}

	public List<String> items() {
		return this.items;
	}

	public boolean exists(ModelFolder folder) {
		return folder.all() || this.children.containsKey(folder) || this.entries.stream().anyMatch(folder::contains);
	}

	public List<ModelEntry> search(String query, String item) {
		String needle = query.trim().toLowerCase(Locale.ROOT);
		List<ModelEntry> out = new ArrayList<>();
		for (ModelEntry entry : this.entries) {
			if (!item.isEmpty() && !item.equals(entry.model().item())) {
				continue;
			}
			if (needle.isEmpty() || this.matches(entry, needle)) {
				out.add(entry);
			}
		}
		return out;
	}

	private boolean matches(ModelEntry entry, String needle) {
		String text = this.haystack.get(entry);
		return text != null && text.contains(needle);
	}

	public List<FolderRow> tree(List<ModelEntry> searched, Set<ModelFolder> open) {
		Map<ModelFolder, Integer> counts = new HashMap<>();
		for (ModelEntry entry : searched) {
			ModelFolder folder = ModelFolder.ALL;
			counts.merge(folder, 1, Integer::sum);
			folder = folder.child(entry.pack());
			counts.merge(folder, 1, Integer::sum);
			if (!entry.folder().isEmpty()) {
				for (String part : entry.folder().split("/")) {
					folder = folder.child(part);
					counts.merge(folder, 1, Integer::sum);
				}
			}
		}
		List<FolderRow> rows = new ArrayList<>();
		this.walk(ModelFolder.ALL, 0, counts, open, rows);
		return rows;
	}

	private void walk(ModelFolder folder, int depth, Map<ModelFolder, Integer> counts, Set<ModelFolder> open, List<FolderRow> rows) {
		List<String> names = folder.all() ? this.packs : new ArrayList<>(this.children.getOrDefault(folder, Set.of()));
		boolean expanded = folder.all() || open.contains(folder);
		rows.add(new FolderRow(folder, depth, counts.getOrDefault(folder, 0), !names.isEmpty(), expanded));
		if (expanded) {
			for (String name : names) {
				this.walk(folder.child(name), depth + 1, counts, open, rows);
			}
		}
	}

	public List<ModelGroup> groups(List<ModelEntry> visible, ModelGrouping grouping, ModelSorting sorting) {
		List<ModelEntry> sorted = new ArrayList<>(visible);
		sorted.sort(this.comparator(sorting));
		Map<String, List<ModelEntry>> grouped = new LinkedHashMap<>();
		for (ModelEntry entry : sorted) {
			grouped.computeIfAbsent(this.key(entry, grouping), key -> new ArrayList<>()).add(entry);
		}
		List<ModelGroup> groups = new ArrayList<>();
		for (Map.Entry<String, List<ModelEntry>> entry : grouped.entrySet()) {
			groups.add(new ModelGroup(entry.getKey(), List.copyOf(entry.getValue())));
		}
		if (grouping == ModelGrouping.FOLDER || grouping == ModelGrouping.PACK) {
			groups.sort(Comparator.comparingInt((ModelGroup group) -> this.packs.indexOf(group.first().pack())).thenComparing(group -> group.first().folder()));
		} else {
			groups.sort(Comparator.comparing(ModelGroup::key));
		}
		return groups;
	}

	private String key(ModelEntry entry, ModelGrouping grouping) {
		switch (grouping) {
			case FOLDER:
				return entry.pack() + "\n" + entry.folder();
			case ITEM_MODEL:
				return name(entry);
			case ITEM:
				return entry.model().item();
			case PACK:
				return entry.pack();
			default:
				return "";
		}
	}

	private Comparator<ModelEntry> comparator(ModelSorting sorting) {
		Comparator<ModelEntry> byName = Comparator.comparing(ModelCatalog::name).thenComparingInt(ModelCatalog::value);
		switch (sorting) {
			case VALUE:
				return Comparator.comparingInt(ModelCatalog::value).thenComparing(byName);
			case PACK_ORDER:
				return Comparator.comparingInt(ModelEntry::order);
			default:
				return byName;
		}
	}

	private static String name(ModelEntry entry) {
		return entry.model().itemModel() != null ? entry.model().itemModel() : entry.model().item();
	}

	private static int value(ModelEntry entry) {
		return entry.model().value() == null ? -1 : entry.model().value();
	}
}
