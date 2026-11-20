package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.PotionEffectUtil;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class EffectFieldsTest {
	private static final GameVersion[] ERAS = {GameVersion.V1_17_1, GameVersion.V1_20_2, GameVersion.V1_20_6, GameVersion.V1_21_5, GameVersion.V26_3};

	@Test
	void potionRoundTrips() {
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("potion", 1, version);
			PotionEffect effect = PotionEffect.create("minecraft:speed").withAmplifier(2).withDuration(PotionEffect.INFINITE).withShowParticles(false);
			PotionContents contents = new PotionContents("minecraft:strong_swiftness", 0x123456, null, List.of(effect));
			ItemFields.POTION_CONTENTS.write(item, version, contents);
			PotionContents read = ItemFields.POTION_CONTENTS.get(item, version);
			assertEquals("minecraft:strong_swiftness", read.potion(), version.toString());
			assertEquals(0x123456, read.customColor());
			PotionEffect back = read.effects().get(0);
			assertEquals("minecraft:speed", back.id());
			assertEquals(2, back.amplifier());
			assertEquals(PotionEffect.INFINITE, back.duration());
			assertFalse(back.showParticles());
			assertTrue(back.showIcon());
			ItemFields.POTION_CONTENTS.write(item, version, null);
			assertFalse(ItemFields.POTION_CONTENTS.has(item, version));
		}
	}

	@Test
	void legacyNumericIds() {
		CompoundTag item = ItemStackUtil.create("potion", 1, GameVersion.V1_17_1);
		ItemFields.POTION_CONTENTS.write(item, GameVersion.V1_17_1, PotionContents.EMPTY.withEffects(List.of(PotionEffect.create("minecraft:night_vision"))));
		CompoundTag stored = item.getCompound("tag").getList("CustomPotionEffects").getCompound(0);
		assertEquals(ByteTag.of((byte) 16), stored.get("Id"));
		assertEquals("minecraft:darkness", PotionEffectUtil.fromLegacyId(33));
	}

	@Test
	void shortComponentForm() {
		CompoundTag item = ItemStackUtil.create("potion", 1, GameVersion.V1_21_5);
		ItemStackUtil.setComponent(item, "potion_contents", StringTag.of("healing"));
		assertEquals("minecraft:healing", ItemFields.POTION_CONTENTS.get(item, GameVersion.V1_21_5).potion());
	}

	@Test
	void stewRoundTrips() {
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("suspicious_stew", 1, version);
			ItemFields.STEW_EFFECTS.write(item, version, List.of(PotionEffect.create("minecraft:blindness").withDuration(100)));
			PotionEffect back = ItemFields.STEW_EFFECTS.get(item, version).get(0);
			assertEquals("minecraft:blindness", back.id(), version.toString());
			assertEquals(100, back.duration());
		}
	}

	@Test
	void trimRoundTrips() {
		for (GameVersion version : new GameVersion[] {GameVersion.V1_20_1, GameVersion.V1_20_6, GameVersion.V26_3}) {
			CompoundTag item = ItemStackUtil.create("diamond_chestplate", 1, version);
			ItemFields.TRIM.write(item, version, new ArmorTrim("minecraft:gold", "minecraft:coast"));
			assertEquals(new ArmorTrim("minecraft:gold", "minecraft:coast"), ItemFields.TRIM.get(item, version));
			ItemFields.TRIM.write(item, version, null);
			assertNull(ItemFields.TRIM.get(item, version));
		}
	}

	@Test
	void durations() {
		assertEquals(40, PotionEffectUtil.parseDuration("2", true));
		assertEquals(1800, PotionEffectUtil.parseDuration("1:30", true));
		assertEquals(7, PotionEffectUtil.parseDuration("7t", true));
		assertEquals(PotionEffect.INFINITE, PotionEffectUtil.parseDuration("∞", true));
		assertNull(PotionEffectUtil.parseDuration("∞", false));
		assertEquals("1:30", PotionEffectUtil.formatDuration(1800));
		assertEquals("7t", PotionEffectUtil.formatDuration(7));
	}
}
