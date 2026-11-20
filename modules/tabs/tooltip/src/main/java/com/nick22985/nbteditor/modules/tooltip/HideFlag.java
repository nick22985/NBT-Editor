package com.nick22985.nbteditor.modules.tooltip;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum HideFlag {
	ENCHANTMENTS(1, null),
	MODIFIERS(2, null),
	UNBREAKABLE(4, null),
	CAN_DESTROY(8, null),
	CAN_PLACE_ON(16, null),
	ADDITIONAL(32, null),
	DYED(64, null),
	TRIM(128, Feature.TRIMS);

	private final int bit;
	private final Feature feature;

	HideFlag(int bit, Feature feature) {
		this.bit = bit;
		this.feature = feature;
	}

	public int bit() {
		return bit;
	}

	public boolean supported(GameVersion version) {
		return feature == null || feature.in(version);
	}
}
