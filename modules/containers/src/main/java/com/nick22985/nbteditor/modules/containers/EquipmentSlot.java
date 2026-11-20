package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.ui.SlotIcon;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum EquipmentSlot {
	HEAD(SlotIcon.HELMET, 0, 3),
	CHEST(SlotIcon.CHESTPLATE, 0, 2),
	LEGS(SlotIcon.LEGGINGS, 0, 1),
	FEET(SlotIcon.BOOTS, 0, 0),
	MAINHAND(SlotIcon.SWORD, 1, 0),
	OFFHAND(SlotIcon.SHIELD, 1, 1),
	BODY(SlotIcon.NONE, 2, -1),
	SADDLE(SlotIcon.SADDLE, 2, -1);

	private final SlotIcon icon;
	private final int column;
	private final int legacyIndex;

	EquipmentSlot(SlotIcon icon, int column, int legacyIndex) {
		this.icon = icon;
		this.column = column;
		this.legacyIndex = legacyIndex;
	}

	public SlotIcon icon() {
		return this.icon;
	}

	public int column() {
		return this.column;
	}

	public int legacyIndex() {
		return this.legacyIndex;
	}

	public boolean armor() {
		return this.column == 0;
	}

	public boolean hand() {
		return this.column == 1;
	}

	public String serializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
