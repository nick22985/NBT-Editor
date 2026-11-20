package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItemSettings {
	public static final int DEFAULT_LIMIT = 54;
	public static final int MAX_LIMIT = 540;

	private final Map<LostItemSource, Setting<Boolean>> enabled = new EnumMap<>(LostItemSource.class);
	private final Map<LostItemSource, Setting<Integer>> limits = new EnumMap<>(LostItemSource.class);
	private final Setting<Integer> playerRange;
	private final Setting<Boolean> persist;

	public LostItemSettings(ModuleContext ctx) {
		for (LostItemSource source : LostItemSource.values()) {
			this.enabled.put(source, ctx.config().bool(source.serializedName(), source.enabledByDefault()));
			this.limits.put(source, ctx.config().integer(source.serializedName() + "_limit", DEFAULT_LIMIT, 1, MAX_LIMIT));
		}
		this.playerRange = ctx.config().integer("player_range", 16, 1, 128);
		this.persist = ctx.config().bool("persist", true);
	}

	public boolean enabled(LostItemSource source) {
		return this.enabled.get(source).get();
	}

	public int limit(LostItemSource source) {
		return this.limits.get(source).get();
	}

	public int playerRange() {
		return this.playerRange.get();
	}

	public boolean persist() {
		return this.persist.get();
	}
}
