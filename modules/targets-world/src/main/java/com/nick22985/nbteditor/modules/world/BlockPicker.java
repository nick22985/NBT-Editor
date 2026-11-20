package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.HeadNames;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.PickListener;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SkullUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockPicker implements PickListener {
	private final ModuleContext ctx;
	private final Setting<Boolean> enabled;
	private final Setting<Boolean> debug;

	public BlockPicker(ModuleContext ctx, Setting<Boolean> enabled, Setting<Boolean> debug) {
		this.ctx = ctx;
		this.enabled = enabled;
		this.debug = debug;
	}

	@Override
	public boolean enabled() {
		return this.enabled.get();
	}

	@Override
	public boolean debug() {
		return this.debug.get();
	}

	@Override
	public GameItem finish(GameItem item) {
		GameVersion version = this.ctx.platform().game().version();
		CompoundTag tag = item.tag();
		if (!SkullUtil.isPlayerHead(ItemStackUtil.id(tag)) || ItemFields.CUSTOM_NAME.has(tag, version)) {
			return item;
		}
		Tag profile = SkullUtil.profile(tag, true, version);
		String owner = SkullUtil.name(profile);
		if (owner != null && !owner.isBlank()) {
			return item;
		}
		Optional<Text> name = this.ctx.services().find(HeadNames.class).flatMap(names -> names.name(SkullUtil.texture(profile)));
		if (name.isEmpty()) {
			return item;
		}
		ItemFields.CUSTOM_NAME.write(tag, version, name.get());
		return this.ctx.client().items().fromTag(tag);
	}

	@Override
	public void picked(BlockData block, GameItem item, boolean includeData) {
		String blockEntity = this.snbt(block.blockEntity());
		String picked = item == null ? null : this.snbt(item.tag());
		this.ctx.log().info("Pick block " + block.stateString() + " include data " + includeData + " block entity " + blockEntity + " item " + picked);
		this.ctx.client().chat().message(this.ctx.tr(includeData ? "pick.debug.block.data" : "pick.debug.block", block.stateString(), blockEntity));
		this.ctx.client().chat().message(item == null ? this.ctx.tr("pick.debug.server") : this.ctx.tr("pick.debug.item", picked));
	}

	private String snbt(CompoundTag tag) {
		return tag == null ? "{}" : SnbtUtil.write(tag, SnbtDialect.MODERN);
	}
}
