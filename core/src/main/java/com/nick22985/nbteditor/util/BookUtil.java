package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.BookContent;
import com.nick22985.nbteditor.data.BookField;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookUtil {
	public static final int PAGE_WIDTH = 114;
	public static final int PAGE_LINES = 14;
	public static final int WRITABLE_PAGE_LENGTH = 1024;

	private BookUtil() {
	}

	public static boolean written(CompoundTag item) {
		return BookField.WRITTEN.equals(ItemStackUtil.id(item));
	}

	public static BookContent read(CompoundTag item, GameVersion version) {
		BookContent content = ItemFields.BOOK.get(item, version);
		return content == null ? BookContent.empty(written(item)) : content;
	}

	public static int maxPages(GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version) ? BookContent.MAX_PAGES : Integer.MAX_VALUE;
	}

	public static void convert(CompoundTag item, GameVersion version) {
		boolean written = written(item);
		BookContent content = read(item, version);
		ItemFields.BOOK.write(item, version, null);
		ItemStackUtil.setId(item, written ? BookField.WRITABLE : BookField.WRITTEN);
		List<Text> pages = new ArrayList<>();
		for (Text page : content.pages()) {
			pages.add(written ? Text.literal(page.plain()) : page);
		}
		ItemFields.BOOK.write(item, version, content.asWritten(!written).withPages(pages));
	}
}
