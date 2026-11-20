package com.nick22985.nbteditor.modules.slotunlock;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotUnlockModule implements Module {
	@Override
	public String id() {
		return "slot-unlock";
	}

	@Override
	public void init(ModuleContext ctx) {
		SlotUnlockSettings settings = new SlotUnlockSettings(ctx);
		ctx.net().register(SlotUnlockPackets.STATE);
		ctx.net().register(SlotUnlockPackets.ACK);
		new SlotUnlockServer(ctx, settings).register();
		if (ctx.isClient()) {
			new SlotUnlockClient(ctx, settings).register();
		}
	}
}
