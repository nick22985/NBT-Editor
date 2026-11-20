package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.TargetResolver;
import com.nick22985.nbteditor.target.WorldTargets;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class WorldTargetsModule implements Module {
	@Override
	public String id() {
		return "targets-world";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		Setting<Boolean> blockUpdates = ctx.config().bool("block_updates", true);
		Setting<Boolean> recreate = ctx.config().bool("recreate", false);
		Setting<Boolean> otherPlayers = ctx.config().bool("other_players", false);
		WorldTargetFactory factory = new WorldTargetFactory(ctx, blockUpdates, recreate, otherPlayers);
		ctx.services().provide(WorldTargets.class, factory);
		ctx.register(TargetResolver.REGISTRY, "world", factory::hit, 100);
		new WorldCommands(ctx, factory).register();
		ctx.client().world().pickListener(new BlockPicker(ctx, ctx.config().bool("client_pick", true), ctx.config().bool("pick_debug", false)));
	}
}
