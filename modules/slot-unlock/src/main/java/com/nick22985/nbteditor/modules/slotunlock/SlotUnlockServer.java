package com.nick22985.nbteditor.modules.slotunlock;

import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.SlotCheckEvent;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.ServerPlatform;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotUnlockServer {
	private final ModuleContext ctx;
	private final SlotUnlockSettings settings;
	private final Map<UUID, Integer> masks = new ConcurrentHashMap<>();

	public SlotUnlockServer(ModuleContext ctx, SlotUnlockSettings settings) {
		this.ctx = ctx;
		this.settings = settings;
	}

	public void register() {
		this.ctx.net().registerServer(SlotUnlockPackets.STATE, (sender, mask) -> this.ctx.platform().scheduler().server(() -> this.state(sender, mask)));
		this.ctx.hooks().on(CoreHooks.PLAYER_LEAVE_SERVER, event -> this.masks.remove(event.player()));
		this.ctx.hooks().on(CoreHooks.SERVER_STOP, event -> this.masks.clear());
		this.ctx.hooks().on(CoreHooks.SLOT_CHECK, this::check);
	}

	private boolean permitted(UUID player) {
		ServerPlatform server = this.ctx.server();
		return server != null && server.running() && server.permissionLevel(player) >= this.settings.permission();
	}

	private void state(UUID sender, int mask) {
		boolean allowed = this.permitted(sender);
		if (allowed && mask != 0) {
			this.masks.put(sender, mask);
		} else {
			this.masks.remove(sender);
		}
		if (this.ctx.net().clientAvailable(sender)) {
			this.ctx.net().sendToClient(sender, SlotUnlockPackets.ACK, allowed);
		}
	}

	private void check(SlotCheckEvent event) {
		if (!event.server() || event.allowed()) {
			return;
		}
		Integer mask = this.masks.get(event.player());
		if (mask != null && (mask & event.rule().bit()) != 0 && this.permitted(event.player())) {
			event.allow();
		}
	}
}
