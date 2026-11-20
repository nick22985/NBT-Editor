package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.WorldBackends;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ServerBridgeModule implements Module {
	@Override
	public String id() {
		return "server-bridge";
	}

	@Override
	public void init(ModuleContext ctx) {
		Setting<Integer> permission = ctx.config().integer("permission_level", 2, 0, 4);
		Setting<Boolean> otherPlayers = ctx.config().bool("other_players", false);
		ctx.net().register(BridgePackets.HELLO);
		ctx.net().register(BridgePackets.WELCOME);
		ctx.net().register(BridgePackets.REQUEST);
		ctx.net().register(BridgePackets.RESPONSE);
		new BridgeServer(ctx, permission, otherPlayers).register();
		if (ctx.isClient()) {
			ServerBridgeBackend backend = new ServerBridgeBackend(ctx, ctx.config().bool("mismatch_warning", true));
			backend.register();
			ctx.services().provide(ServerBridgeBackend.class, backend);
			ctx.services().get(WorldBackends.class).add(backend);
		}
	}
}
