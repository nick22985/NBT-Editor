package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItemHistory {
	private static final String FILE = "lost_items.dat";
	private static final int DEDUPE_DEPTH = 8;

	private final ModuleContext ctx;
	private final LostItemSettings settings;
	private final Map<LostItemSource, LinkedList<LostItemEntry>> entries = new EnumMap<>(LostItemSource.class);
	private boolean loaded;
	private boolean dirty;

	public LostItemHistory(ModuleContext ctx, LostItemSettings settings) {
		this.ctx = ctx;
		this.settings = settings;
		for (LostItemSource source : LostItemSource.values()) {
			this.entries.put(source, new LinkedList<>());
		}
	}

	private Path file() {
		return DataFileUtil.dataDir(this.ctx.platform().game().gameDir()).resolve(FILE);
	}

	public void load() {
		if (this.loaded) {
			return;
		}
		this.loaded = true;
		if (!this.settings.persist()) {
			return;
		}
		try {
			CompoundTag root = DataFileUtil.read(this.file());
			ListTag list = root == null ? null : root.getList("entries");
			if (list == null) {
				return;
			}
			int current = this.ctx.client().items().dataVersion();
			for (Tag element : list) {
				if (!(element instanceof CompoundTag)) {
					continue;
				}
				LostItemEntry entry = LostItemEntry.fromTag((CompoundTag) element);
				if (entry == null) {
					continue;
				}
				if (entry.dataVersion() < current) {
					entry = new LostItemEntry(entry.source(), this.ctx.client().items().upgrade(entry.item(), entry.dataVersion()), current, entry.time());
				}
				this.entries.get(entry.source()).addLast(entry);
			}
		} catch (IOException | RuntimeException e) {
			this.ctx.log().warn("Could not read lost item history", e);
		}
	}

	public void save() {
		if (!this.dirty) {
			return;
		}
		this.dirty = false;
		if (!this.settings.persist()) {
			return;
		}
		ListTag list = new ListTag();
		for (LinkedList<LostItemEntry> values : this.entries.values()) {
			for (LostItemEntry entry : values) {
				list.add(entry.toTag());
			}
		}
		CompoundTag root = new CompoundTag();
		root.put("entries", list);
		try {
			DataFileUtil.write(this.file(), root);
		} catch (IOException e) {
			this.ctx.log().warn("Could not save lost item history", e);
		}
	}

	public boolean add(LostItemSource source, GameItem item) {
		if (!this.settings.enabled(source) || !InventoryUtil.present(item)) {
			return false;
		}
		this.load();
		CompoundTag tag = item.tag();
		LinkedList<LostItemEntry> list = this.entries.get(source);
		Iterator<LostItemEntry> iterator = list.iterator();
		for (int i = 0; i < DEDUPE_DEPTH && iterator.hasNext(); i++) {
			if (iterator.next().item().equals(tag)) {
				iterator.remove();
				break;
			}
		}
		list.addFirst(new LostItemEntry(source, tag, this.ctx.client().items().dataVersion(), System.currentTimeMillis()));
		int limit = this.settings.limit(source);
		while (list.size() > limit) {
			list.removeLast();
		}
		this.dirty = true;
		return true;
	}

	public List<LostItemEntry> entries(LostItemSource source) {
		this.load();
		LinkedList<LostItemEntry> list = this.entries.get(source);
		int limit = this.settings.limit(source);
		while (list.size() > limit) {
			list.removeLast();
			this.dirty = true;
		}
		return new ArrayList<>(list);
	}

	public LostItemEntry latest() {
		this.load();
		LostItemEntry latest = null;
		for (LinkedList<LostItemEntry> list : this.entries.values()) {
			if (!list.isEmpty() && (latest == null || list.getFirst().time() > latest.time())) {
				latest = list.getFirst();
			}
		}
		return latest;
	}

	public void clear(LostItemSource source) {
		this.load();
		this.entries.get(source).clear();
		this.dirty = true;
	}

	public void clearAll() {
		this.load();
		for (LinkedList<LostItemEntry> list : this.entries.values()) {
			list.clear();
		}
		this.dirty = true;
	}
}
