package com.nick22985.nbteditor.modules.slotunlock;

import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.SlotCheckEvent;
import com.nick22985.nbteditor.hook.SlotRule;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotUnlockClient {
	private static final int UNSENT = -1;

	private final ModuleContext ctx;
	private final SlotUnlockSettings settings;
	private int sent = UNSENT;
	private boolean granted;

	public SlotUnlockClient(ModuleContext ctx, SlotUnlockSettings settings) {
		this.ctx = ctx;
		this.settings = settings;
	}

	public void register() {
		this.ctx.net().registerClient(SlotUnlockPackets.ACK, allowed -> this.ctx.platform().scheduler().client(() -> this.granted = allowed));
		this.ctx.hooks().on(CoreHooks.CLIENT_TICK, event -> this.tick(event.inWorld()));
		this.ctx.hooks().on(CoreHooks.WORLD_JOIN, event -> this.reset());
		this.ctx.hooks().on(CoreHooks.WORLD_LEAVE, event -> this.reset());
		this.ctx.hooks().on(CoreHooks.SLOT_CHECK, this::check);
	}

	private void reset() {
		this.sent = UNSENT;
		this.granted = false;
	}

	private void tick(boolean inWorld) {
		if (!inWorld || !this.ctx.net().serverAvailable()) {
			return;
		}
		int mask = this.settings.mask();
		if (mask != this.sent) {
			this.sent = mask;
			this.ctx.net().sendToServer(SlotUnlockPackets.STATE, mask);
		}
	}

	private void check(SlotCheckEvent event) {
		if (event.server() || event.allowed()) {
			return;
		}
		SlotRule rule = event.rule();
		if (!this.settings.rule(rule)) {
			return;
		}
		if (this.granted && this.sent != UNSENT || rule == SlotRule.ARMOR && this.ctx.client().player().creative()) {
			event.allow();
		}
	}
}
