package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.text.Formatting;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum HeadCategory {
	ALL("all", Formatting.WHITE, false),
	FAVORITES("favorites", Formatting.YELLOW, false),
	PLAYERS("players", Formatting.GREEN, false),
	ALPHABET("alphabet", Formatting.AQUA, true),
	ANIMALS("animals", Formatting.GOLD, true),
	BLOCKS("blocks", Formatting.GRAY, true),
	DECORATION("decoration", Formatting.BLUE, true),
	FOOD_DRINKS("food-drinks", Formatting.RED, true),
	HUMANS("humans", Formatting.DARK_AQUA, true),
	HUMANOID("humanoid", Formatting.LIGHT_PURPLE, true),
	MISCELLANEOUS("miscellaneous", Formatting.WHITE, true),
	MONSTERS("monsters", Formatting.DARK_RED, true),
	PLANTS("plants", Formatting.DARK_GREEN, true);

	private final String id;
	private final Formatting color;
	private final boolean remote;

	HeadCategory(String id, Formatting color, boolean remote) {
		this.id = id;
		this.color = color;
		this.remote = remote;
	}

	public String id() {
		return this.id;
	}

	public String langKey() {
		return "category." + this.id.replace('-', '_');
	}

	public Formatting color() {
		return this.color;
	}

	public boolean remote() {
		return this.remote;
	}
}
