package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class DataCommandUtilTest {
	private static final String TARGET = "block 1 2 3";

	@Test
	void smallChangesBecomeSingleCommands() {
		CompoundTag before = new CompoundTag().putInt("a", 1).putString("gone", "x").putString("same", "y");
		CompoundTag after = new CompoundTag().putInt("a", 2).putString("same", "y").putString("new key", "z");
		List<String> commands = DataCommandUtil.diff("", TARGET, before, after, SnbtDialect.MODERN);
		assertEquals(List.of("data remove block 1 2 3 gone", "data modify block 1 2 3 a set value 2", "data modify block 1 2 3 \"new key\" set value \"z\""), commands);
	}

	@Test
	void unchangedProducesNothing() {
		CompoundTag tag = new CompoundTag().putInt("a", 1);
		assertTrue(DataCommandUtil.diff("", TARGET, tag, tag.copy(), SnbtDialect.MODERN).isEmpty());
	}

	@Test
	void largeCompoundsAreSplit() {
		CompoundTag inner = new CompoundTag();
		for (int i = 0; i < 40; i++) {
			inner.putString("key" + i, "value number " + i);
		}
		CompoundTag after = new CompoundTag().put("inner", inner);
		List<String> commands = DataCommandUtil.diff("", TARGET, new CompoundTag(), after, SnbtDialect.MODERN);
		assertEquals("data modify block 1 2 3 inner set value {}", commands.get(0));
		assertEquals(41, commands.size());
		commands.forEach(command -> assertTrue(command.length() <= DataCommandUtil.MAX_LENGTH));
		assertEquals("data modify block 1 2 3 inner.key0 set value \"value number 0\"", commands.get(1));
	}

	@Test
	void largeListsAreRebuiltByAppending() {
		ListTag items = new ListTag();
		for (int i = 0; i < 30; i++) {
			items.add(new CompoundTag().putString("id", "minecraft:stone").putInt("Slot", i));
		}
		List<String> commands = DataCommandUtil.diff("execute in minecraft:overworld run ", TARGET, new CompoundTag(), new CompoundTag().put("Items", items), SnbtDialect.MODERN);
		assertEquals("execute in minecraft:overworld run data modify block 1 2 3 Items set value []", commands.get(0));
		assertEquals(31, commands.size());
		assertTrue(commands.get(1).endsWith("Items append value {id:\"minecraft:stone\",Slot:0}"));
	}

	@Test
	void sameSizeListsUpdateByIndex() {
		ListTag before = new ListTag();
		ListTag after = new ListTag();
		for (int i = 0; i < 30; i++) {
			before.add(StringTag.of("entry number " + i));
			after.add(StringTag.of(i == 7 ? "changed" : "entry number " + i));
		}
		List<String> commands = DataCommandUtil.diff("", TARGET, new CompoundTag().put("l", before), new CompoundTag().put("l", after), SnbtDialect.MODERN);
		assertEquals(List.of("data modify block 1 2 3 l[7] set value \"changed\""), commands);
	}

	@Test
	void arraysAppendElements() {
		int[] values = new int[60];
		for (int i = 0; i < values.length; i++) {
			values[i] = 1000000 + i;
		}
		List<String> commands = DataCommandUtil.diff("", TARGET, new CompoundTag(), new CompoundTag().put("a", new IntArrayTag(values)), SnbtDialect.MODERN);
		assertEquals("data modify block 1 2 3 a set value [I;]", commands.get(0));
		assertEquals("data modify block 1 2 3 a append value 1000000", commands.get(1));
	}

	@Test
	void hugeStringsFail() {
		Tag huge = StringTag.of("x".repeat(400));
		assertThrows(IllegalArgumentException.class, () -> DataCommandUtil.diff("", TARGET, new CompoundTag(), new CompoundTag().put("s", huge), SnbtDialect.MODERN));
	}

	@Test
	void sectionSignsAreEscapedOnlyInModern() {
		assertEquals("\"\\u00a7cRed\"", DataCommandUtil.chatSafe("\"§cRed\"", SnbtDialect.MODERN));
		assertNull(DataCommandUtil.chatSafe("\"§cRed\"", SnbtDialect.LEGACY));
	}

	@Test
	void itemArguments() {
		CompoundTag modern = ItemStackUtil.create("diamond_sword", 1, GameVersion.V1_21_5);
		ItemStackUtil.setComponent(modern, "damage", IntTag.of(5));
		modern.getCompound("components").put("!minecraft:food", new CompoundTag());
		assertEquals("item replace entity @s hotbar.0 with minecraft:diamond_sword[minecraft:damage=5,!minecraft:food] 1", ItemCommandUtil.replaceCommand("@s", 0, modern, GameVersion.V1_21_5, SnbtDialect.MODERN));
		CompoundTag legacy = ItemStackUtil.create("stone", 3, GameVersion.V1_17_1);
		legacy.put("tag", new CompoundTag().putInt("CustomModelData", 2));
		assertEquals("item replace entity @s armor.head with minecraft:stone{CustomModelData:2} 3", ItemCommandUtil.replaceCommand("@s", 39, legacy, GameVersion.V1_17_1, SnbtDialect.LEGACY));
	}
}
