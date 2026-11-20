package com.nick22985.nbteditor.modules.vanilla;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackends;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class VanillaBridgeModule implements Module {
	@Override
	public String id() {
		return "vanilla-bridge";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		Setting<Boolean> enabled = ctx.config().bool("enabled", true);
		CommandQueue queue = new CommandQueue(ctx);
		VanillaBackend backend = new VanillaBackend(ctx, queue, enabled);
		ctx.hooks().on(CoreHooks.CLIENT_TICK, event -> queue.tick());
		ctx.hooks().on(CoreHooks.CHAT_RECEIVE, -100, queue::receive);
		ctx.hooks().on(CoreHooks.WORLD_LEAVE, event -> {
			queue.failAll(new TargetException(ctx.tr("error.disconnected")));
			backend.reset();
		});
		ctx.services().get(WorldBackends.class).add(backend);
	}
}
