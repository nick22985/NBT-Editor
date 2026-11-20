package com.nick22985.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.data.ModelOption;
import com.nick22985.nbteditor.data.PackModel;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.platform.ResourceService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class ItemModelUtilTest {
	@Test
	void legacyOverrides() {
		String json = "{\"parent\":\"item/handheld\",\"overrides\":[{\"predicate\":{\"custom_model_data\":2},\"model\":\"pack:item/ruby_sword\"},{\"predicate\":{\"custom_model_data\":1.5},\"model\":\"pack:item/half\"},{\"predicate\":{\"pulling\":1},\"model\":\"item/bow_pulling_0\"}]}";
		assertEquals(List.of(new ModelOption(2, "pack:item/half")), ItemModelUtil.overrideOptions(json));
	}

	@Test
	void definitionRangeDispatch() {
		String json = "{\"model\":{\"type\":\"minecraft:range_dispatch\",\"property\":\"minecraft:custom_model_data\",\"entries\":[{\"threshold\":10,\"model\":{\"type\":\"minecraft:model\",\"model\":\"pack:item/ten\"}},{\"threshold\":3,\"model\":{\"type\":\"condition\",\"property\":\"using_item\",\"on_true\":{\"type\":\"model\",\"model\":\"a\"},\"on_false\":{\"type\":\"model\",\"model\":\"b\"}}}],\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"minecraft:item/stick\"}}}";
		assertEquals(List.of(new ModelOption(3, "condition"), new ModelOption(10, "pack:item/ten")), ItemModelUtil.definitionOptions(json));
	}

	@Test
	void definitionIgnoresOtherIndexes() {
		String json = "{\"model\":{\"type\":\"range_dispatch\",\"property\":\"custom_model_data\",\"index\":1,\"entries\":[{\"threshold\":4,\"model\":{\"type\":\"model\",\"model\":\"x\"}}]}}";
		assertTrue(ItemModelUtil.definitionOptions(json).isEmpty());
		assertTrue(ItemModelUtil.definitionOptions("not json").isEmpty());
	}

	@Test
	void readsFromResources() {
		Map<String, String> files = Map.of("pack:items/tools/ruby.json", "{}", "minecraft:items/stick.json", "{\"model\":{\"type\":\"range_dispatch\",\"property\":\"custom_model_data\",\"entries\":[{\"threshold\":1,\"model\":{\"type\":\"model\",\"model\":\"one\"}}]}}", "minecraft:models/item/stick.json", "{\"overrides\":[{\"predicate\":{\"custom_model_data\":7},\"model\":\"seven\"}]}");
		ResourceService resources = new MapResourceService(files);

		assertEquals(List.of(new ModelOption(1, "one")), ItemModelUtil.customModelData(resources, "stick", GameVersion.V1_21_4));
		assertEquals(List.of(new ModelOption(7, "seven")), ItemModelUtil.customModelData(resources, "minecraft:stick", GameVersion.V1_20_1));
		assertEquals(List.of("minecraft:stick", "pack:tools/ruby"), ItemModelUtil.itemModels(resources, GameVersion.V1_21_4));
	}

	@Test
	void catalogListsPackModels() {
		String stick = "{\"model\":{\"type\":\"range_dispatch\",\"property\":\"custom_model_data\",\"entries\":[{\"threshold\":1,\"model\":{\"type\":\"model\",\"model\":\"one\"}}]}}";
		Map<String, String> files = Map.of("pack:items/ruby.json", "{}", "minecraft:items/stick.json", stick, "minecraft:items/apple.json", "{}");
		List<PackModel> models = ItemModelUtil.catalog(new MapResourceService(files), GameVersion.V1_21_4, id -> id.equals("minecraft:stick") || id.equals("minecraft:apple"));
		assertEquals(List.of(new PackModel("minecraft:stick", null, 1, "one"), new PackModel("minecraft:paper", "pack:ruby", null, "")), models);
		assertEquals("minecraft:stick #1", models.get(0).label());
		assertEquals("pack:ruby", models.get(1).label());
	}

	@Test
	void catalogUsesOverridesOnOldVersions() {
		Map<String, String> files = Map.of("minecraft:models/item/stick.json", "{\"overrides\":[{\"predicate\":{\"custom_model_data\":7},\"model\":\"seven\"}]}", "pack:models/item/ruby.json", "{}", "minecraft:models/item/generated.json", "{}");
		assertEquals(List.of(new PackModel("minecraft:stick", null, 7, "seven")), ItemModelUtil.catalog(new MapResourceService(files), GameVersion.V1_20_1, id -> id.equals("minecraft:stick")));
		assertEquals(List.of(new PackModel("minecraft:stick", null, 7, "seven"), new PackModel("minecraft:paper", "pack:ruby", null, "")), ItemModelUtil.catalog(new MapResourceService(files), GameVersion.V1_21_2, id -> id.equals("minecraft:stick")));
	}

	@Test
	void entriesKnowTheirPackAndFolder() {
		String sword = "{\"model\":{\"type\":\"range_dispatch\",\"property\":\"custom_model_data\",\"entries\":[{\"threshold\":1001,\"model\":{\"type\":\"model\",\"model\":\"fw:item/weapons/swords/flame\"}}]}}";
		Map<String, String> files = Map.of("fw:items/weapons/katana.json", "{}", "minecraft:items/diamond_sword.json", sword);
		List<ModelEntry> entries = ItemModelUtil.entries(new MapResourceService(files), GameVersion.V1_21_4, id -> id.equals("minecraft:diamond_sword"));
		assertEquals(List.of(new ModelEntry(new PackModel("minecraft:paper", "fw:weapons/katana", null, ""), "fw", "weapons", 0), new ModelEntry(new PackModel("minecraft:diamond_sword", null, 1001, "fw:item/weapons/swords/flame"), "minecraft", "weapons/swords", 1)), entries);
	}

	@Test
	void folders() {
		assertEquals("", ItemModelUtil.folder(new PackModel("minecraft:paper", "pack:ruby", 1, "pack:item/a/b")));
		assertEquals("hats", ItemModelUtil.folder(new PackModel("minecraft:paper", "cosm:hats/crown", 2, "")));
		assertEquals("decor", ItemModelUtil.folder(new PackModel("minecraft:stick", null, 2, "cosm:block/decor/lantern")));
		assertEquals("", ItemModelUtil.folder(new PackModel("minecraft:stick", null, 2, "condition")));
		assertEquals("Fantasy Weapons", ItemModelUtil.packTitle("file/Fantasy Weapons.ZIP"));
		assertEquals("", ItemModelUtil.packTitle(null));
	}

	@Test
	void buildsCatalogItems() {
		CompoundTag item = ItemModelUtil.item(new PackModel("minecraft:paper", "pack:ruby", 3, ""), GameVersion.V1_21_4);
		assertEquals("minecraft:paper", ItemStackUtil.id(item));
		assertEquals(StringTag.of("pack:ruby"), ItemStackUtil.component(item, "item_model"));
		assertEquals(3, ItemFields.CUSTOM_MODEL_DATA.get(item, GameVersion.V1_21_4));
	}
}
