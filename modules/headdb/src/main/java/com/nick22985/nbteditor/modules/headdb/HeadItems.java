package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SkullUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadItems {
	private final ModuleContext ctx;

	public HeadItems(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public GameItem create(Head head) {
		GameVersion version = this.ctx.platform().game().version();
		CompoundTag tag = ItemStackUtil.create(SkullUtil.PLAYER_HEAD, 1, version);
		SkullUtil.setProfile(tag, true, version, SkullUtil.texturedProfile(head.uuid(), head.value(), version));
		ItemFields.CUSTOM_NAME.write(tag, version, this.name(head));
		if (!head.tags().isEmpty()) {
			ItemFields.LORE.write(tag, version, List.of(Text.literal(String.join(", ", head.tags())).withStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))));
		}
		return this.ctx.client().items().fromTag(tag);
	}

	public Text name(Head head) {
		return Text.literal(head.name()).withStyle(Style.EMPTY.withItalic(false).withColor(head.category().color()));
	}
}
