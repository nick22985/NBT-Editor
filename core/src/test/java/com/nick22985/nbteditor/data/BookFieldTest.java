package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BookUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.RichTextUtil;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class BookFieldTest {
	private static final GameVersion[] ERAS = {GameVersion.V1_17_1, GameVersion.V1_20_4, GameVersion.V1_20_6, GameVersion.V1_21_5, GameVersion.V26_3};

	@Test
	void writtenRoundTrips() {
		Text red = Text.literal("Hi", Style.EMPTY.withColor(Formatting.RED));
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("written_book", 1, version);
			ItemFields.BOOK.write(item, version, new BookContent(true, "Tale", "nick", 2, true, List.of(red, Text.literal("two"))));
			BookContent read = ItemFields.BOOK.get(item, version);
			assertTrue(read.written());
			assertEquals("Tale", read.title(), version.toString());
			assertEquals("nick", read.author());
			assertEquals(2, read.generation());
			assertTrue(read.resolved());
			assertEquals(2, read.pages().size());
			assertTrue(RichTextUtil.same(red, read.pages().get(0)), version.toString());
			ItemFields.BOOK.write(item, version, null);
			assertFalse(ItemFields.BOOK.has(item, version));
		}
	}

	@Test
	void writableRoundTrips() {
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("writable_book", 1, version);
			ItemFields.BOOK.write(item, version, BookContent.empty(false).withPages(List.of(Text.literal("a\nb"))));
			BookContent read = ItemFields.BOOK.get(item, version);
			assertFalse(read.written());
			assertEquals("a\nb", read.pages().get(0).plain(), version.toString());
		}
	}

	@Test
	void convertKeepsPages() {
		for (GameVersion version : ERAS) {
			CompoundTag item = ItemStackUtil.create("writable_book", 1, version);
			ItemFields.BOOK.write(item, version, BookContent.empty(false).withPages(List.of(Text.literal("page"))));
			BookUtil.convert(item, version);
			assertEquals(BookField.WRITTEN, ItemStackUtil.id(item));
			assertEquals("page", BookUtil.read(item, version).pages().get(0).plain(), version.toString());
			BookUtil.convert(item, version);
			assertEquals(BookField.WRITABLE, ItemStackUtil.id(item));
			assertEquals("page", BookUtil.read(item, version).pages().get(0).plain());
		}
	}
}
