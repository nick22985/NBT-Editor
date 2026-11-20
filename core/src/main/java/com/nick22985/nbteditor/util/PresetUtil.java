package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PresetUtil {
	private PresetUtil() {
	}

	public static CompoundTag colorCodes(Text name, GameVersion version) {
		CompoundTag item = ItemStackUtil.create("minecraft:paper", 1, version);
		Style base = Style.EMPTY.withItalic(false);
		ItemFields.CUSTOM_NAME.write(item, version, name.withStyle(base.withColor(Formatting.GOLD).withBold(true)));
		List<Text> lore = new ArrayList<>();
		for (Formatting formatting : Formatting.values()) {
			String label = "&" + formatting.code() + "  " + formatting.serializedName().replace('_', ' ').toLowerCase(Locale.ROOT);
			Style style = formatting.isColor() ? base.withColor(formatting) : base.withColor(Formatting.WHITE).apply(formatting);
			if (formatting == Formatting.OBFUSCATED) {
				lore.add(Text.join(List.of(Text.literal("&k  ", base.withColor(Formatting.WHITE)), Text.literal("obfuscated", style))));
			} else {
				lore.add(Text.literal(label, style));
			}
		}
		ItemFields.LORE.write(item, version, lore);
		return item;
	}
}
