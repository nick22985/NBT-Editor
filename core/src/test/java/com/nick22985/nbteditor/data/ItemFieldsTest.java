package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.FloatTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.ShortTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class ItemFieldsTest {
	private static final GameVersion[] ERAS = {GameVersion.V1_17_1, GameVersion.V1_20_6, GameVersion.V1_21_4, GameVersion.V1_21_5, GameVersion.V26_3};

	@Test
	void namePerEra() {
		CompoundTag legacy = ItemStackUtil.create("stone", 1, GameVersion.V1_17_1);
		ItemFields.CUSTOM_NAME.write(legacy, GameVersion.V1_17_1, Text.literal("Hi"));
		assertEquals(StringTag.of("\"Hi\""), legacy.getCompound("tag").getCompound("display").get("Name"));

		CompoundTag components = ItemStackUtil.create("stone", 1, GameVersion.V1_20_6);
		ItemFields.CUSTOM_NAME.write(components, GameVersion.V1_20_6, Text.literal("Hi"));
		assertEquals(StringTag.of("\"Hi\""), components.getCompound("components").get("minecraft:custom_name"));

		CompoundTag modern = ItemStackUtil.create("stone", 1, GameVersion.V1_21_5);
		ItemFields.CUSTOM_NAME.write(modern, GameVersion.V1_21_5, Text.literal("Hi"));
		assertEquals(StringTag.of("Hi"), modern.getCompound("components").get("minecraft:custom_name"));

		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("stone", 1, version);
			ItemFields.CUSTOM_NAME.write(item, version, Text.literal("Name"));
			assertEquals(Text.literal("Name"), ItemFields.CUSTOM_NAME.get(item, version), version.name());
			ItemFields.CUSTOM_NAME.remove(item, version);
			assertFalse(ItemFields.CUSTOM_NAME.has(item, version), version.name());
			assertEquals(ItemStackUtil.create("stone", 1, version), item, version.name());
		}
	}

	@Test
	void countPerEra() {
		assertTrue(ItemStackUtil.create("stone", 3, GameVersion.V1_17_1).contains("Count"));
		assertTrue(ItemStackUtil.create("stone", 3, GameVersion.V1_20_6).contains("count"));
		assertEquals(99, ItemStackUtil.maxCount(GameVersion.V1_21_1));
		assertEquals(127, ItemStackUtil.count(ItemStackUtil.create("stone", 300, GameVersion.V1_17_1)));
	}

	@Test
	void loreAndEnchantmentsRoundTrip() {
		Map<String, Integer> levels = new LinkedHashMap<>();
		levels.put("sharpness", 5);
		levels.put("minecraft:unbreaking", 3);
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("diamond_sword", 1, version);
			ItemFields.LORE.write(item, version, List.of(Text.literal("a"), Text.literal("b")));
			ItemFields.ENCHANTMENTS.write(item, version, levels);
			assertEquals(List.of(Text.literal("a"), Text.literal("b")), ItemFields.LORE.get(item, version), version.name());
			Map<String, Integer> read = ItemFields.ENCHANTMENTS.get(item, version);
			assertEquals(5, read.get("minecraft:sharpness"), version.name());
			assertEquals(3, read.get("minecraft:unbreaking"), version.name());
		}
		CompoundTag legacy = ItemStackUtil.create("diamond_sword", 1, GameVersion.V1_17_1);
		ItemFields.ENCHANTMENTS.write(legacy, GameVersion.V1_17_1, Map.of("sharpness", 1000));
		assertEquals(ShortTag.of((short) 1000), legacy.getCompound("tag").getList("Enchantments").getCompound(0).get("lvl"));
		CompoundTag component = ItemStackUtil.create("diamond_sword", 1, GameVersion.V1_21_4);
		ItemFields.ENCHANTMENTS.write(component, GameVersion.V1_21_4, Map.of("sharpness", 1000));
		assertEquals(1000, ItemFields.ENCHANTMENTS.get(component, GameVersion.V1_21_4).get("minecraft:sharpness"));
		assertTrue(component.getCompound("components").getCompound("minecraft:enchantments").contains("levels"));
	}

	@Test
	void customModelDataLists() {
		CompoundTag item = ItemStackUtil.create("stick", 1, GameVersion.V1_21_4);
		ItemFields.CUSTOM_MODEL_DATA.write(item, GameVersion.V1_21_4, 7);
		CompoundTag cmd = item.getCompound("components").getCompound("minecraft:custom_model_data");
		assertEquals(ListTag.of(FloatTag.of(7f)), cmd.getList("floats"));
		assertEquals(7, ItemFields.CUSTOM_MODEL_DATA.get(item, GameVersion.V1_21_4));
	}

	@Test
	void invalidValueIsReportedNotThrown() {
		CompoundTag item = ItemStackUtil.create("stick", 1, GameVersion.V1_20_6);
		ItemStackUtil.setComponent(item, "damage", StringTag.of("lots"));
		FieldValue<Integer> value = ItemFields.DAMAGE.read(item, GameVersion.V1_20_6);
		assertTrue(value.present());
		assertFalse(value.ok());
		assertNull(value.value());
	}

	@Test
	void containerPerEra() {
		for (GameVersion version : ERAS) {
			CompoundTag chest = ItemStackUtil.create("chest", 1, version);
			List<ContainerSlot> slots = List.of(new ContainerSlot(0, ItemStackUtil.create("stone", 2, version)), new ContainerSlot(26, ItemStackUtil.create("dirt", 1, version)));
			ItemFields.CONTAINER.write(chest, version, slots);
			assertEquals(slots, ItemFields.CONTAINER.get(chest, version), version.name());
		}
		CompoundTag modern = ItemStackUtil.create("chest", 1, GameVersion.V1_21_5);
		ItemFields.CONTAINER.write(modern, GameVersion.V1_21_5, List.of(new ContainerSlot(4, ItemStackUtil.create("stone", 1, GameVersion.V1_21_5))));
		assertInstanceOf(CompoundTag.class, modern.getCompound("components").getList("minecraft:container").getCompound(0).get("item"));
	}

	@Test
	void versionLookup() {
		assertEquals(GameVersion.V1_21_5, GameVersion.byDataVersion(4325));
		assertTrue(GameVersion.byDataVersion(4400).atLeast(GameVersion.V1_21_5));
		assertTrue(Feature.TEXT_AS_NBT.in(GameVersion.V26_3));
		assertFalse(Feature.ITEM_COMPONENTS.in(GameVersion.V1_20_4));
	}
}
