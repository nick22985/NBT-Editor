package com.nick22985.nbteditor.data;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum MobFlag {
	PERSISTENCE_REQUIRED("PersistenceRequired"),
	NO_AI("NoAI"),
	CAN_PICK_UP_LOOT("CanPickUpLoot"),
	LEFT_HANDED("LeftHanded"),
	SILENT("Silent"),
	GLOWING("Glowing"),
	INVULNERABLE("Invulnerable"),
	CUSTOM_NAME_VISIBLE("CustomNameVisible"),
	NO_GRAVITY("NoGravity");

	private final String key;

	MobFlag(String key) {
		this.key = key;
	}

	public String key() {
		return this.key;
	}

	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
