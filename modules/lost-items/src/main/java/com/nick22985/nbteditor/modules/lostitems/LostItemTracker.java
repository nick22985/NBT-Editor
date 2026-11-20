package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.ItemLostEvent;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItemTracker {
	private static final int CONTAINER_INTERVAL = 20;
	private static final int PLAYER_INTERVAL = 40;
	private static final int SAVE_INTERVAL = 200;
	private static final int MAX_SEEN = 4096;

	private final ModuleContext ctx;
	private final LostItemSettings settings;
	private final LostItemHistory history;
	private final Map<LostItemSource, Set<CompoundTag>> seen = new EnumMap<>(LostItemSource.class);
	private Map<CompoundTag, Integer> counts = new HashMap<>();
	private Map<CompoundTag, GameItem> samples = new HashMap<>();
	private int ticks;
	private String cursorId;
	private int cursorCount;
	private int inventoryChanges = -1;
	private int playerId = -1;

	public LostItemTracker(ModuleContext ctx, LostItemSettings settings, LostItemHistory history) {
		this.ctx = ctx;
		this.settings = settings;
		this.history = history;
	}

	public void register() {
		this.ctx.hooks().on(CoreHooks.ITEM_LOST, this::lost);
		this.ctx.hooks().on(CoreHooks.CLIENT_TICK, event -> this.tick(event.inWorld()));
		this.ctx.hooks().on(CoreHooks.WORLD_LEAVE, event -> this.leave());
	}

	private void lost(ItemLostEvent event) {
		LostItemSource source = LostItemSource.byName(event.reason().serializedName());
		if (source != null) {
			this.history.add(source, event.item());
		}
	}

	private void leave() {
		this.cursorId = null;
		this.inventoryChanges = -1;
		this.playerId = -1;
		this.counts = new HashMap<>();
		this.samples = new HashMap<>();
		this.seen.clear();
		this.history.save();
	}

	private void tick(boolean inWorld) {
		PlayerService player = this.ctx.client().player();
		if (!inWorld || !player.present()) {
			return;
		}
		this.ticks++;
		if (this.settings.enabled(LostItemSource.CURSOR)) {
			this.cursor(player);
		}
		if (this.settings.enabled(LostItemSource.INVENTORY)) {
			this.inventory(player);
		}
		if (this.settings.enabled(LostItemSource.CONTAINER) && this.ticks % CONTAINER_INTERVAL == 0) {
			this.seen(LostItemSource.CONTAINER, player.menuItems());
		}
		if (this.settings.enabled(LostItemSource.PLAYERS) && this.ticks % PLAYER_INTERVAL == 0) {
			this.seen(LostItemSource.PLAYERS, this.ctx.client().world().nearbyPlayerItems(this.settings.playerRange()));
		}
		if (this.ticks % SAVE_INTERVAL == 0) {
			this.history.save();
		}
	}

	private void cursor(PlayerService player) {
		GameItem cursor = player.cursor();
		if (!InventoryUtil.present(cursor)) {
			this.cursorId = null;
			return;
		}
		if (cursor.id().equals(this.cursorId) && cursor.count() == this.cursorCount) {
			return;
		}
		this.cursorId = cursor.id();
		this.cursorCount = cursor.count();
		this.history.add(LostItemSource.CURSOR, cursor);
	}

	private void inventory(PlayerService player) {
		int id = player.networkId();
		int changes = player.inventoryChanges();
		if (id == this.playerId && changes == this.inventoryChanges) {
			return;
		}
		Map<CompoundTag, Integer> oldCounts = this.counts;
		Map<CompoundTag, GameItem> oldSamples = this.samples;
		boolean compare = id == this.playerId;
		this.playerId = id;
		this.inventoryChanges = changes;
		this.counts = new HashMap<>();
		this.samples = new HashMap<>();
		for (int slot = 0; slot <= PlayerService.OFFHAND_SLOT; slot++) {
			GameItem item = player.slot(slot);
			if (InventoryUtil.present(item)) {
				CompoundTag key = InventoryUtil.withoutCount(item.tag());
				this.counts.merge(key, item.count(), Integer::sum);
				this.samples.putIfAbsent(key, item);
			}
		}
		if (compare) {
			this.diff(oldCounts, oldSamples);
		}
	}

	private void diff(Map<CompoundTag, Integer> oldCounts, Map<CompoundTag, GameItem> oldSamples) {
		Map<String, Integer> gained = new HashMap<>();
		for (Map.Entry<CompoundTag, Integer> entry : this.counts.entrySet()) {
			int gain = entry.getValue() - oldCounts.getOrDefault(entry.getKey(), 0);
			if (gain > 0) {
				gained.merge(this.samples.get(entry.getKey()).id(), gain, Integer::sum);
			}
		}
		for (Map.Entry<CompoundTag, Integer> entry : oldCounts.entrySet()) {
			int loss = entry.getValue() - this.counts.getOrDefault(entry.getKey(), 0);
			if (loss <= 0) {
				continue;
			}
			GameItem sample = oldSamples.get(entry.getKey());
			int replaced = gained.getOrDefault(sample.id(), 0);
			if (replaced > 0) {
				gained.put(sample.id(), Math.max(0, replaced - loss));
				continue;
			}
			this.history.add(LostItemSource.INVENTORY, InventoryUtil.withCount(this.ctx.client().items(), sample, loss));
		}
	}

	private void seen(LostItemSource source, List<GameItem> items) {
		Set<CompoundTag> seen = this.seen.computeIfAbsent(source, key -> new HashSet<>());
		for (GameItem item : items) {
			if (!InventoryUtil.present(item)) {
				continue;
			}
			if (seen.size() >= MAX_SEEN) {
				seen.clear();
			}
			if (seen.add(item.tag())) {
				this.history.add(source, item);
			}
		}
	}
}
