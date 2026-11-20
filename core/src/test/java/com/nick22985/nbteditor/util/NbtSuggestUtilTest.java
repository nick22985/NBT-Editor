package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.editor.NbtSuggestPrefix;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.StringTag;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class NbtSuggestUtilTest {
	@Test
	void prefixWalksCompoundsAndLists() {
		ListTag items = new ListTag();
		items.add(new CompoundTag().putString("id", "minecraft:stone"));
		CompoundTag root = new CompoundTag().put("Items", items);
		NbtSuggestPrefix key = NbtSuggestUtil.prefix(root, List.of("Items", 0), "Co", null);
		assertEquals("{Items:[{Co", key.snbt());
		assertEquals(9, key.fieldStart());
		NbtSuggestPrefix value = NbtSuggestUtil.prefix(root, List.of("Items", 0), "id", "\"minecraft:st");
		assertEquals("{Items:[{id:\"minecraft:st", value.snbt());
		assertEquals(12, value.fieldStart());
		NbtSuggestPrefix element = NbtSuggestUtil.prefix(root, List.of("Items"), "", "{");
		assertEquals("{Items:[{", element.snbt());
		assertNull(NbtSuggestUtil.prefix(root, List.of("Items"), "x", null));
	}

	@Test
	void quotedKeysStayOpenOnlyWhileTyping() {
		CompoundTag root = new CompoundTag();
		assertEquals("{\"my ke", NbtSuggestUtil.prefix(root, List.of(), "my ke", null).snbt());
		assertEquals("{\"my key\":1", NbtSuggestUtil.prefix(root, List.of(), "my key", "1").snbt());
		assertNull(NbtSuggestUtil.prefix(new CompoundTag().put("s", StringTag.of("x")), List.of("s"), "a", "1"));
	}

	@Test
	void closersOnlyInsideTheValue() {
		assertTrue(NbtSuggestUtil.closesOutside(",", "1"));
		assertTrue(NbtSuggestUtil.closesOutside("]", "[1,2]"));
		assertFalse(NbtSuggestUtil.closesOutside("}", "{a:1"));
		assertFalse(NbtSuggestUtil.closesOutside(",", "[\"a]\""));
		assertEquals(1, NbtSuggestUtil.depth("[\"]\""));
	}

	@Test
	void componentNamesMatchEveryForm() {
		String id = "minecraft:custom_name";
		assertTrue(NbtSuggestUtil.componentMatches("cust", id));
		assertTrue(NbtSuggestUtil.componentMatches("minecraft:cu", id));
		assertTrue(NbtSuggestUtil.componentMatches(":cu", id));
		assertTrue(NbtSuggestUtil.componentMatches("!cu", id));
		assertFalse(NbtSuggestUtil.componentMatches("lore", id));
		assertTrue(NbtSuggestUtil.componentTaken(List.of(":custom_name"), id));
		assertTrue(NbtSuggestUtil.componentTaken(List.of("!custom_name"), id));
		assertFalse(NbtSuggestUtil.componentTaken(List.of("lore"), id));
	}

	@Test
	void unquoteHandlesEscapes() {
		assertEquals("a \"b\"", NbtSuggestUtil.unquote("\"a \\\"b\\\"\""));
		assertEquals("plain", NbtSuggestUtil.unquote("plain"));
	}
}
