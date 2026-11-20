package com.nick22985.nbteditor.data;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum Feature {
	SIGN_TWO_SIDES(GameVersion.V1_20),
	TRIMS(GameVersion.V1_20),
	CHEST_BOATS(GameVersion.V1_19),
	ALLAYS(GameVersion.V1_19),
	BUNDLES(GameVersion.V1_17),
	CONTAINER_ITEMS_IN_ENTITIES(GameVersion.V1_20_3),
	INFINITE_EFFECTS(GameVersion.V1_19_4),
	STRING_EFFECT_IDS(GameVersion.V1_20_2),
	ITEM_COMPONENTS(GameVersion.V1_20_5),
	BODY_ARMOR_ITEM(GameVersion.V1_20_5),
	BLOCK_ENTITY_COMPONENTS(GameVersion.V1_20_5),
	ITEM_COUNT_ABOVE_127(GameVersion.V1_20_5),
	CUSTOM_MODEL_DATA_LISTS(GameVersion.V1_21_4),
	ITEM_MODEL_COMPONENT(GameVersion.V1_21_2),
	POTION_CUSTOM_NAME(GameVersion.V1_21_2),
	SHADOW_COLOR(GameVersion.V1_21_4),
	MODERN_SNBT(GameVersion.V1_21_5),
	TEXT_AS_NBT(GameVersion.V1_21_5),
	SNAKE_CASE_TEXT_EVENTS(GameVersion.V1_21_5),
	ENTITY_EQUIPMENT(GameVersion.V1_21_5),
	MOB_DROP_CHANCES(GameVersion.V1_21_5),
	TOOLTIP_DISPLAY(GameVersion.V1_21_5),
	BLOCK_POS_COMPOUND_FIELDS(GameVersion.V1_21_5),
	DIALOGS(GameVersion.V1_21_6),
	OBJECT_TEXT(GameVersion.V1_21_9),
	UNOBFUSCATED(GameVersion.V26_1);

	private final GameVersion since;

	Feature(GameVersion since) {
		this.since = since;
	}

	public GameVersion since() {
		return since;
	}

	public boolean in(GameVersion version) {
		return version.atLeast(since);
	}
}
