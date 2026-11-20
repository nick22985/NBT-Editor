package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.text.Text;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class MiscUtilTest {
	private static final GameVersion[] ERAS = {GameVersion.V1_17_1, GameVersion.V1_20_6, GameVersion.V26_3};

	@Test
	void unbindKeepsTexture() {
		for (GameVersion version : ERAS) {
			boolean modern = version.atLeast(GameVersion.V1_20_5);
			CompoundTag item = ItemStackUtil.create(SkullUtil.PLAYER_HEAD, 1, version);
			CompoundTag profile = new CompoundTag();
			profile.putString(modern ? "name" : "Name", "Steve");
			profile.put(modern ? "id" : "Id", UuidUtil.toIntArray(UUID.randomUUID()));
			if (modern) {
				CompoundTag property = new CompoundTag();
				property.putString("name", "textures");
				property.putString("value", "abc");
				profile.put("properties", ListTag.of(property));
			} else {
				CompoundTag texture = new CompoundTag();
				texture.putString("Value", "abc");
				CompoundTag properties = new CompoundTag();
				properties.put("textures", ListTag.of(texture));
				profile.put("Properties", properties);
			}
			SkullUtil.setProfile(item, true, version, profile);
			assertEquals("abc", SkullUtil.texture(SkullUtil.profile(item, true, version)), version.toString());
			assertTrue(SkullUtil.unbind(item, true, version));
			assertEquals(SkullUtil.UNBOUND_NAME, SkullUtil.name(SkullUtil.profile(item, true, version)));
			assertEquals(new UUID(0L, 0L), SkullUtil.uuid(SkullUtil.profile(item, true, version)));
			assertEquals("abc", SkullUtil.texture(SkullUtil.profile(item, true, version)));
			assertTrue(ItemFields.CUSTOM_NAME.has(item, version));
		}
	}

	@Test
	void customDataUuid() {
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("stone", 1, version);
			UUID uuid = UUID.randomUUID();
			ItemDataUtil.putCustomData(item, version, "UUID", UuidUtil.toIntArray(uuid));
			assertEquals(uuid, UuidUtil.fromTag(ItemDataUtil.customData(item, version, "UUID")));
			ItemDataUtil.putCustomData(item, version, "UUID", null);
			assertNull(ItemDataUtil.customData(item, version, "UUID"));
			assertEquals(ItemStackUtil.create("stone", 1, version), item);
		}
	}

	@Test
	void signatureToggles() {
		GameVersion version = GameVersion.V26_3;
		CompoundTag item = ItemStackUtil.create("stone", 1, version);
		Text signature = Text.literal("by me");
		assertTrue(SignatureUtil.add(item, version, signature));
		assertFalse(SignatureUtil.add(item, version, signature));
		assertTrue(SignatureUtil.replace(item, version, signature, Text.literal("by you")));
		assertTrue(SignatureUtil.remove(item, version, Text.literal("by you")));
		assertEquals(ItemStackUtil.create("stone", 1, version), item);
	}

	@Test
	void blockStatesFollowContents() {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("facing", "north");
		properties.put("has_book", "false");
		CompoundTag lectern = new CompoundTag();
		lectern.put("Book", ItemStackUtil.create("written_book", 1, GameVersion.V26_3));
		BlockData synced = BlockStateUtil.sync(new BlockData(BlockStateUtil.LECTERN, properties, lectern));
		assertEquals("true", synced.properties().get("has_book"));
		assertEquals("north", synced.properties().get("facing"));
		CompoundTag stand = new CompoundTag();
		CompoundTag bottle = ItemStackUtil.create("potion", 1, GameVersion.V26_3);
		bottle.putByte("Slot", (byte) 2);
		stand.put("Items", ListTag.of(bottle));
		Map<String, String> bottles = new LinkedHashMap<>(Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "false"));
		BlockData brewing = BlockStateUtil.sync(new BlockData(BlockStateUtil.BREWING_STAND, bottles, stand));
		assertEquals(List.of("false", "false", "true"), List.of(brewing.properties().get("has_bottle_0"), brewing.properties().get("has_bottle_1"), brewing.properties().get("has_bottle_2")));
	}
}
