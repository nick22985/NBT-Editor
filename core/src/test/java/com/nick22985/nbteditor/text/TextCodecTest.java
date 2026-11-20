package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.TextCodecUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class TextCodecTest {
	private static Text sample() {
		CompoundTag components = new CompoundTag();
		components.putInt("minecraft:damage", 3);
		Style style = Style.EMPTY.withColor(Formatting.GOLD).withBold(true).withItalic(false)
			.withClickEvent(new ClickEvent(ClickAction.RUN_COMMAND, "/say hi"))
			.withHoverEvent(HoverEvent.showItem("minecraft:diamond_sword", 1, components));
		return Text.literal("Hello", style)
			.append(Text.literal(" world", Style.EMPTY.withColor(TextColor.rgb(0x123456)).withShadowColor(0xFF00FF00)))
			.append(Text.translatable("item.minecraft.diamond", Text.literal("arg"), 5))
			.append(Text.literal("!", Style.EMPTY.withHoverEvent(HoverEvent.showEntity("minecraft:pig", UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"), Text.literal("Pig")))));
	}

	@Test
	void plainStringIsLiteral() {
		assertEquals("\"plain\"", TextCodecUtil.toJson(Text.literal("plain"), TextDialect.JSON_TAG_ITEMS));
		assertEquals(StringTag.of("plain"), TextCodecUtil.toNbt(Text.literal("plain")));
	}

	@Test
	void legacyJsonUsesCamelCaseEvents() {
		String json = TextCodecUtil.toJson(sample(), TextDialect.JSON_TAG_ITEMS);
		assertTrue(json.contains("\"clickEvent\""), json);
		assertTrue(json.contains("\"hoverEvent\""), json);
		assertTrue(json.contains("\"contents\""), json);
		assertTrue(json.contains("\"tag\":\"{\\\"minecraft:damage\\\":3}\""), json);
	}

	@Test
	void modernUsesSnakeCaseEvents() {
		String json = TextCodecUtil.toJson(sample(), TextDialect.NBT);
		assertTrue(json.contains("\"click_event\""), json);
		assertTrue(json.contains("\"command\":\"/say hi\""), json);
		assertTrue(json.contains("\"hover_event\""), json);
		assertTrue(json.contains("\"uuid\""), json);
	}

	@Test
	void emptyItemHoverIsDroppedWithComponents() {
		Text text = Text.literal("x", Style.EMPTY.withHoverEvent(HoverEvent.showItem("minecraft:air", 1, null)));
		assertTrue(TextCodecUtil.toJson(text, TextDialect.JSON_TAG_ITEMS).contains("hoverEvent"));
		assertFalse(TextCodecUtil.toJson(text, TextDialect.JSON_COMPONENT_ITEMS).contains("hoverEvent"));
		assertFalse(TextCodecUtil.toJson(text, TextDialect.NBT).contains("hover_event"));
	}

	@Test
	void roundTripEveryDialect() {
		Text text = sample();
		for (TextDialect dialect : TextDialect.values()) {
			Tag stored = TextCodecUtil.toStored(text, dialect);
			if (dialect.storesAsNbt()) {
				assertInstanceOf(CompoundTag.class, stored);
			} else {
				assertInstanceOf(StringTag.class, stored);
			}
			assertEquals(text, TextCodecUtil.fromStored(stored, dialect), dialect.name());
		}
	}

	@Test
	void readsOldShowItemValueForm() {
		Text text = TextCodecUtil.fromJson("{\"text\":\"x\",\"hoverEvent\":{\"action\":\"show_item\",\"value\":\"{id:\\\"minecraft:stone\\\",Count:2b}\"}}");
		HoverEvent hover = text.style().hoverEvent();
		assertEquals("minecraft:stone", hover.id());
		assertEquals(2, hover.count());
	}

	@Test
	void invalidJsonFallsBackToLiteral() {
		assertEquals(Text.literal("{not json"), TextCodecUtil.fromJson("{not json"));
	}

	@Test
	void unknownContentIsKept() {
		Text text = TextCodecUtil.fromJson("{\"type\":\"object\",\"object\":\"atlas\",\"sprite\":\"item/apple\",\"color\":\"red\"}");
		assertInstanceOf(RawContent.class, text.content());
		assertEquals(TextColor.of(Formatting.RED), text.style().color());
		Text back = TextCodecUtil.fromNbt(TextCodecUtil.toNbt(text));
		assertEquals(text, back);
	}

	@Test
	void listFormAppendsSiblings() {
		Text text = TextCodecUtil.fromJson("[\"a\",{\"text\":\"b\",\"bold\":true}]");
		assertEquals("ab", text.plain());
		assertEquals(1, text.siblings().size());
	}

	@Test
	void nbtBooleansAsBytes() {
		CompoundTag tag = new CompoundTag().putString("text", "x").putBoolean("bold", true);
		assertEquals(Boolean.TRUE, TextCodecUtil.fromNbt(tag).style().bold());
	}
}
