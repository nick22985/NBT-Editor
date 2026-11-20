package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.TextCodecUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LoreField implements Field<List<Text>> {
	@Override
	public String id() {
		return "lore";
	}

	@Override
	public FieldValue<List<Text>> read(CompoundTag item, GameVersion version) {
		Tag tag = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(item, "lore") : ItemDataUtil.display(item, false) == null ? null : ItemDataUtil.display(item, false).get("Lore");
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		TextDialect dialect = TextDialect.of(version);
		List<Text> lines = new ArrayList<>();
		for (Tag line : (ListTag) tag) {
			lines.add(TextCodecUtil.fromStored(line, dialect));
		}
		return FieldValue.of(lines);
	}

	@Override
	public void write(CompoundTag item, GameVersion version, List<Text> value) {
		ListTag list = null;
		if (value != null && !value.isEmpty()) {
			TextDialect dialect = TextDialect.of(version);
			list = new ListTag();
			for (Text line : value) {
				list.add(TextCodecUtil.toStored(line, dialect));
			}
		}
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, "lore", list);
		} else {
			ItemDataUtil.putDisplay(item, version, "Lore", list);
		}
	}
}
