package com.nick22985.nbteditor.data;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PotionContents(String potion, Integer customColor, String customName, List<PotionEffect> effects) {
	public static final PotionContents EMPTY = new PotionContents(null, null, null, List.of());

	public PotionContents withPotion(String potion) {
		return new PotionContents(potion, this.customColor, this.customName, this.effects);
	}

	public PotionContents withCustomColor(Integer customColor) {
		return new PotionContents(this.potion, customColor, this.customName, this.effects);
	}

	public PotionContents withCustomName(String customName) {
		return new PotionContents(this.potion, this.customColor, customName, this.effects);
	}

	public PotionContents withEffects(List<PotionEffect> effects) {
		return new PotionContents(this.potion, this.customColor, this.customName, effects);
	}

	public boolean isEmpty() {
		return this.potion == null && this.customColor == null && this.customName == null && this.effects.isEmpty();
	}
}
