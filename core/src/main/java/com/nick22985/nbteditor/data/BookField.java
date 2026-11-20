package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.TextCodecUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookField implements Field<BookContent> {
	public static final String WRITTEN = "minecraft:written_book";
	public static final String WRITABLE = "minecraft:writable_book";
	private static final String WRITTEN_COMPONENT = "written_book_content";
	private static final String WRITABLE_COMPONENT = "writable_book_content";

	@Override
	public String id() {
		return "book";
	}

	private boolean written(CompoundTag item) {
		return WRITTEN.equals(ItemStackUtil.id(item));
	}

	@Override
	public FieldValue<BookContent> read(CompoundTag item, GameVersion version) {
		boolean written = this.written(item);
		TextDialect dialect = TextDialect.of(version);
		CompoundTag source;
		if (Feature.ITEM_COMPONENTS.in(version)) {
			Tag tag = ItemStackUtil.component(item, written ? WRITTEN_COMPONENT : WRITABLE_COMPONENT);
			if (tag == null) {
				return FieldValue.absent();
			}
			if (!(tag instanceof CompoundTag)) {
				return FieldValue.invalid("not_compound");
			}
			source = (CompoundTag) tag;
		} else {
			source = ItemStackUtil.data(item, version);
			if (source == null || !source.contains("pages") && !source.contains("title")) {
				return FieldValue.absent();
			}
		}
		List<Text> pages = new ArrayList<>();
		ListTag list = source.getList("pages");
		if (list != null) {
			for (Tag page : list) {
				Tag raw = this.unfilter(page);
				if (written) {
					Text text = TextCodecUtil.fromStored(raw, dialect);
					pages.add(text == null ? Text.EMPTY : text);
				} else {
					pages.add(Text.literal(raw instanceof StringTag ? ((StringTag) raw).value() : ""));
				}
			}
		}
		if (!written) {
			return FieldValue.of(new BookContent(false, null, null, 0, false, pages));
		}
		Tag title = this.unfilter(source.get("title"));
		Tag generation = source.get("generation");
		return FieldValue.of(new BookContent(true, title instanceof StringTag ? ((StringTag) title).value() : "", source.getString("author", ""), generation instanceof NumericTag ? ((NumericTag) generation).intValue() : 0, source.getBoolean("resolved", false), pages));
	}

	private Tag unfilter(Tag tag) {
		if (tag instanceof CompoundTag && ((CompoundTag) tag).contains("raw")) {
			return ((CompoundTag) tag).get("raw");
		}
		return tag;
	}

	@Override
	public void write(CompoundTag item, GameVersion version, BookContent value) {
		boolean written = this.written(item);
		boolean components = Feature.ITEM_COMPONENTS.in(version);
		if (value == null) {
			if (components) {
				ItemStackUtil.setComponent(item, written ? WRITTEN_COMPONENT : WRITABLE_COMPONENT, null);
			} else {
				CompoundTag data = ItemStackUtil.data(item, version);
				if (data != null) {
					for (String key : List.of("pages", "filtered_pages", "title", "filtered_title", "author", "generation", "resolved")) {
						data.remove(key);
					}
					ItemStackUtil.pruneData(item, version);
				}
			}
			return;
		}
		TextDialect dialect = TextDialect.of(version);
		ListTag pages = new ListTag();
		for (Text page : value.pages()) {
			pages.add(written ? TextCodecUtil.toStored(page, dialect) : StringTag.of(page.plain()));
		}
		CompoundTag target;
		if (components) {
			Tag existing = ItemStackUtil.component(item, written ? WRITTEN_COMPONENT : WRITABLE_COMPONENT);
			target = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
		} else {
			target = ItemStackUtil.dataOrCreate(item, version);
			target.remove("filtered_pages");
			target.remove("filtered_title");
		}
		target.put("pages", pages);
		if (written) {
			target.putString("title", value.title() == null ? "" : value.title());
			target.putString("author", value.author() == null ? "" : value.author());
			if (value.generation() == 0 && components) {
				target.remove("generation");
			} else {
				target.putInt("generation", value.generation());
			}
			if (value.resolved()) {
				target.putBoolean("resolved", true);
			} else {
				target.remove("resolved");
			}
		}
		if (components) {
			ItemStackUtil.setComponent(item, written ? WRITTEN_COMPONENT : WRITABLE_COMPONENT, target);
		}
	}
}
