package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.Target;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemTargetFactory implements ItemTargets {
	private final ModuleContext ctx;

	public ItemTargetFactory(ModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public Target held(Hand hand) {
		return new SlotItemTarget(this.ctx, this.ctx.client().player().heldSlot(hand));
	}

	@Override
	public Target slot(int slot) {
		return new SlotItemTarget(this.ctx, slot);
	}

	@Override
	public Target cursor() {
		return new CursorItemTarget(this.ctx);
	}

	@Override
	public Target nested(Target parent, NbtPath path) {
		return new NestedItemTarget(this.ctx, parent, path);
	}
}
