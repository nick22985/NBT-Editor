package com.nick22985.nbteditor.data;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum DropChanceSlot {
	HEAD("head", DropChanceSlot.ARMOR, 3, 4),
	CHEST("chest", DropChanceSlot.ARMOR, 2, 4),
	LEGS("legs", DropChanceSlot.ARMOR, 1, 4),
	FEET("feet", DropChanceSlot.ARMOR, 0, 4),
	MAINHAND("mainhand", DropChanceSlot.HAND, 0, 2),
	OFFHAND("offhand", DropChanceSlot.HAND, 1, 2),
	BODY("body", null, -1, 0);

	public static final String ARMOR = "ArmorDropChances";
	public static final String HAND = "HandDropChances";

	private final String id;
	private final String legacyList;
	private final int legacyIndex;
	private final int legacySize;

	DropChanceSlot(String id, String legacyList, int legacyIndex, int legacySize) {
		this.id = id;
		this.legacyList = legacyList;
		this.legacyIndex = legacyIndex;
		this.legacySize = legacySize;
	}

	public String id() {
		return this.id;
	}

	public String legacyList() {
		return this.legacyList;
	}

	public int legacyIndex() {
		return this.legacyIndex;
	}

	public int legacySize() {
		return this.legacySize;
	}

	public boolean in(GameVersion version) {
		return this != BODY || Feature.BODY_ARMOR_ITEM.in(version);
	}
}
