package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.Hooks;
import com.nick22985.nbteditor.hook.PlayerJoinServerEvent;
import com.nick22985.nbteditor.hook.PlayerLeaveServerEvent;
import com.nick22985.nbteditor.hook.ServerStartEvent;
import com.nick22985.nbteditor.hook.ServerStopEvent;
import com.nick22985.nbteditor.net.NetService;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricServerHooks {
	private final Supplier<Hooks> hooks;
	private final Supplier<NetService> net;
	private final McRegistryAccess registries;
	private final FabricScheduler scheduler;
	private final boolean tickScheduler;

	public FabricServerHooks(Supplier<Hooks> hooks, Supplier<NetService> net, McRegistryAccess registries, FabricScheduler scheduler, boolean tickScheduler) {
		this.hooks = hooks;
		this.net = net;
		this.registries = registries;
		this.scheduler = scheduler;
		this.tickScheduler = tickScheduler;
	}

	public void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> this.registries.server(server));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> this.fire(hooks -> hooks.fire(CoreHooks.SERVER_START, new ServerStartEvent(server.isDedicatedServer()))));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.fire(hooks -> hooks.fire(CoreHooks.SERVER_STOP, new ServerStopEvent())));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> this.registries.server(null));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (this.tickScheduler) {
				this.scheduler.tick();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			UUID uuid = handler.player.getUUID();
			String name = handler.player.getName().getString();
			this.fire(hooks -> hooks.fire(CoreHooks.PLAYER_JOIN_SERVER, new PlayerJoinServerEvent(uuid, name)));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.player.getUUID();
			NetService net = this.net.get();
			if (net != null) {
				net.forget(uuid);
			}
			this.fire(hooks -> hooks.fire(CoreHooks.PLAYER_LEAVE_SERVER, new PlayerLeaveServerEvent(uuid)));
		});
	}

	private void fire(Consumer<Hooks> action) {
		Hooks hooks = this.hooks.get();
		if (hooks != null) {
			action.accept(hooks);
		}
	}
}
