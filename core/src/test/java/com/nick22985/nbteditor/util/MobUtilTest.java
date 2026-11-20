package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.DropChanceSlot;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.MobFlag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.FloatTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class MobUtilTest {
	private static final GameVersion[] ERAS = {GameVersion.V1_17_1, GameVersion.V1_20_4, GameVersion.V1_20_6, GameVersion.V1_21_4, GameVersion.V1_21_5, GameVersion.V26_3};

	private static CompoundTag parse(String snbt, SnbtDialect dialect) throws Exception {
		return SnbtUtil.parseCompound(snbt, dialect);
	}

	@Test
	void legacyListsAreReadInSlotOrder() throws Exception {
		CompoundTag mob = parse("{id:\"minecraft:zombie\",ArmorDropChances:[0.1f,0.2f,0.3f,0.4f],HandDropChances:[0.5f,0.6f]}", SnbtDialect.LEGACY);
		assertEquals(0.1F, MobUtil.dropChance(mob, DropChanceSlot.FEET, GameVersion.V1_20_4));
		assertEquals(0.2F, MobUtil.dropChance(mob, DropChanceSlot.LEGS, GameVersion.V1_20_4));
		assertEquals(0.3F, MobUtil.dropChance(mob, DropChanceSlot.CHEST, GameVersion.V1_20_4));
		assertEquals(0.4F, MobUtil.dropChance(mob, DropChanceSlot.HEAD, GameVersion.V1_20_4));
		assertEquals(0.5F, MobUtil.dropChance(mob, DropChanceSlot.MAINHAND, GameVersion.V1_20_4));
		assertEquals(0.6F, MobUtil.dropChance(mob, DropChanceSlot.OFFHAND, GameVersion.V1_20_4));
	}

	@Test
	void legacyWriteFillsDefaultsAndKeepsOtherSlots() throws Exception {
		CompoundTag mob = parse("{HandDropChances:[0.5f,0.6f]}", SnbtDialect.LEGACY);
		MobUtil.setDropChance(mob, DropChanceSlot.HEAD, GameVersion.V1_17_1, 2.0F, false);
		MobUtil.setDropChance(mob, DropChanceSlot.OFFHAND, GameVersion.V1_17_1, 1.0F, false);
		assertEquals(ListTag.of(FloatTag.of(MobUtil.DEFAULT_DROP_CHANCE), FloatTag.of(MobUtil.DEFAULT_DROP_CHANCE), FloatTag.of(MobUtil.DEFAULT_DROP_CHANCE), FloatTag.of(2.0F)), mob.getList(DropChanceSlot.ARMOR));
		assertEquals(ListTag.of(FloatTag.of(0.5F), FloatTag.of(1.0F)), mob.getList(DropChanceSlot.HAND));
		assertFalse(mob.contains(MobUtil.DROP_CHANCES));
	}

	@Test
	void legacySparseWriteDropsDefaultLists() {
		CompoundTag mob = new CompoundTag();
		MobUtil.setDropChance(mob, DropChanceSlot.MAINHAND, GameVersion.V1_20_4, 0.5F, true);
		assertTrue(mob.contains(DropChanceSlot.HAND));
		MobUtil.setDropChance(mob, DropChanceSlot.MAINHAND, GameVersion.V1_20_4, MobUtil.DEFAULT_DROP_CHANCE, true);
		assertTrue(mob.isEmpty());
		MobUtil.setDropChance(mob, DropChanceSlot.MAINHAND, GameVersion.V1_20_4, MobUtil.DEFAULT_DROP_CHANCE, false);
		assertEquals(2, mob.getList(DropChanceSlot.HAND).size());
	}

	@Test
	void bodyChanceIsAFloatBefore1_21_5() {
		CompoundTag mob = new CompoundTag();
		assertEquals(MobUtil.DEFAULT_DROP_CHANCE, MobUtil.dropChance(mob, DropChanceSlot.BODY, GameVersion.V1_21_4));
		MobUtil.setDropChance(mob, DropChanceSlot.BODY, GameVersion.V1_21_4, 0.75F, false);
		assertEquals(0.75F, mob.getFloat(MobUtil.BODY_ARMOR_DROP_CHANCE, 0));
		assertEquals(0.75F, MobUtil.dropChance(mob, DropChanceSlot.BODY, GameVersion.V1_20_6));
		MobUtil.setDropChance(mob, DropChanceSlot.BODY, GameVersion.V1_21_4, MobUtil.DEFAULT_DROP_CHANCE, true);
		assertTrue(mob.isEmpty());
	}

	@Test
	void modernCompoundOmitsDefaults() throws Exception {
		CompoundTag mob = parse("{id:\"minecraft:zombie\",drop_chances:{head:2.0f}}", SnbtDialect.MODERN);
		assertEquals(2.0F, MobUtil.dropChance(mob, DropChanceSlot.HEAD, GameVersion.V1_21_5));
		assertEquals(MobUtil.DEFAULT_DROP_CHANCE, MobUtil.dropChance(mob, DropChanceSlot.BODY, GameVersion.V26_3));
		MobUtil.setDropChance(mob, DropChanceSlot.BODY, GameVersion.V26_3, 0.0F, false);
		MobUtil.setDropChance(mob, DropChanceSlot.MAINHAND, GameVersion.V26_3, 1.0F, false);
		assertEquals(parse("{head:2.0f,body:0.0f,mainhand:1.0f}", SnbtDialect.MODERN), mob.getCompound(MobUtil.DROP_CHANCES));
		MobUtil.setDropChance(mob, DropChanceSlot.HEAD, GameVersion.V26_3, MobUtil.DEFAULT_DROP_CHANCE, false);
		MobUtil.setDropChance(mob, DropChanceSlot.BODY, GameVersion.V26_3, MobUtil.DEFAULT_DROP_CHANCE, false);
		MobUtil.setDropChance(mob, DropChanceSlot.MAINHAND, GameVersion.V26_3, MobUtil.DEFAULT_DROP_CHANCE, false);
		assertFalse(mob.contains(MobUtil.DROP_CHANCES));
		assertFalse(mob.contains(DropChanceSlot.ARMOR));
		assertFalse(mob.contains(MobUtil.BODY_ARMOR_DROP_CHANCE));
	}

	@Test
	void everySlotRoundTripsInEveryEra() {
		for (GameVersion version : ERAS) {
			CompoundTag mob = new CompoundTag();
			List<DropChanceSlot> slots = MobUtil.slots(version);
			assertEquals(version.atLeast(GameVersion.V1_20_5), slots.contains(DropChanceSlot.BODY), version.toString());
			for (int i = 0; i < slots.size(); i++) {
				MobUtil.setDropChance(mob, slots.get(i), version, i / 10.0F, false);
			}
			for (int i = 0; i < slots.size(); i++) {
				assertEquals(i / 10.0F, MobUtil.dropChance(mob, slots.get(i), version), version + " " + slots.get(i));
			}
			assertEquals(version.atLeast(GameVersion.V1_21_5), mob.contains(MobUtil.DROP_CHANCES), version.toString());
		}
	}

	@Test
	void flagsAndHealth() {
		CompoundTag mob = new CompoundTag();
		for (MobFlag flag : MobFlag.values()) {
			assertFalse(MobUtil.flag(mob, flag));
			MobUtil.setFlag(mob, flag, true, true);
			assertTrue(MobUtil.flag(mob, flag));
		}
		MobUtil.setFlag(mob, MobFlag.NO_AI, false, false);
		assertEquals(0, mob.getNumber("NoAI").intValue());
		MobUtil.setFlag(mob, MobFlag.NO_AI, false, true);
		assertFalse(mob.contains("NoAI"));
		assertNull(MobUtil.health(mob));
		MobUtil.setHealth(mob, 40.0F);
		assertEquals(40.0F, MobUtil.health(mob));
		MobUtil.setHealth(mob, null);
		assertFalse(mob.contains(MobUtil.HEALTH));
	}

	@Test
	void spawnEggDataPerEra() {
		for (GameVersion version : ERAS) {
			boolean components = version.atLeast(GameVersion.V1_20_5);
			CompoundTag egg = ItemStackUtil.create("minecraft:zombie_spawn_egg", 1, version);
			assertEquals("minecraft:zombie", MobUtil.entityId(egg, true, version));
			CompoundTag data = MobUtil.read(egg, true, version);
			MobUtil.setFlag(data, MobFlag.NO_AI, true, true);
			MobUtil.setDropChance(data, DropChanceSlot.HEAD, version, 2.0F, true);
			MobUtil.write(egg, true, version, data);
			Tag stored = components ? ItemStackUtil.component(egg, MobUtil.ITEM_COMPONENT) : ItemDataUtil.dataTag(egg, version, MobUtil.ITEM_LEGACY);
			assertInstanceOf(CompoundTag.class, stored, version.toString());
			CompoundTag entity = (CompoundTag) stored;
			assertEquals(components ? "minecraft:zombie" : null, entity.getString("id"), version.toString());
			assertTrue(MobUtil.flag(entity, MobFlag.NO_AI));
			assertEquals(2.0F, MobUtil.dropChance(MobUtil.read(egg, true, version), DropChanceSlot.HEAD, version));
			CompoundTag cleared = MobUtil.read(egg, true, version);
			MobUtil.setFlag(cleared, MobFlag.NO_AI, false, true);
			MobUtil.setDropChance(cleared, DropChanceSlot.HEAD, version, MobUtil.DEFAULT_DROP_CHANCE, true);
			MobUtil.write(egg, true, version, cleared);
			assertEquals(ItemStackUtil.create("minecraft:zombie_spawn_egg", 1, version), egg, version.toString());
		}
	}

	@Test
	void spawnEggKeepsOverriddenEntity() {
		GameVersion version = GameVersion.V26_3;
		CompoundTag egg = ItemStackUtil.create("minecraft:zombie_spawn_egg", 1, version);
		CompoundTag data = new CompoundTag();
		data.putString("id", "minecraft:skeleton");
		MobUtil.write(egg, true, version, data);
		assertEquals("minecraft:skeleton", MobUtil.entityId(egg, true, version));
		assertEquals("minecraft:skeleton", ((CompoundTag) ItemStackUtil.component(egg, MobUtil.ITEM_COMPONENT)).getString("id"));
	}

	@Test
	void entityDataIsEditedInPlace() throws Exception {
		CompoundTag root = parse("{id:\"minecraft:zombie\",Health:20.0f,PersistenceRequired:0b}", SnbtDialect.LEGACY);
		assertTrue(MobUtil.looksLikeMob(root));
		assertFalse(MobUtil.looksLikeMob(parse("{id:\"minecraft:item\"}", SnbtDialect.LEGACY)));
		assertEquals("minecraft:zombie", MobUtil.entityId(root, false, GameVersion.V1_20_4));
		CompoundTag data = MobUtil.read(root, false, GameVersion.V1_20_4);
		MobUtil.setFlag(data, MobFlag.PERSISTENCE_REQUIRED, true, false);
		data.remove(MobUtil.HEALTH);
		MobUtil.write(root, false, GameVersion.V1_20_4, data);
		assertEquals(parse("{id:\"minecraft:zombie\",PersistenceRequired:1b}", SnbtDialect.LEGACY), root);
	}

	@Test
	void spawnEggIds() {
		assertTrue(MobUtil.isSpawnEgg("minecraft:zombie_spawn_egg"));
		assertFalse(MobUtil.isSpawnEgg("minecraft:egg"));
		assertEquals("minecraft:mooshroom", MobUtil.eggEntity("mooshroom_spawn_egg"));
		assertEquals("othermod:thing", MobUtil.eggEntity("othermod:thing_spawn_egg"));
		assertNull(MobUtil.eggEntity("minecraft:stone"));
	}
}
