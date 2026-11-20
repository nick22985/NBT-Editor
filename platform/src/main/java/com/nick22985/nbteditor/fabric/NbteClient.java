package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.fabric.client.FabricClientHooks;
import com.nick22985.nbteditor.fabric.client.FabricClientPlatform;
import com.nick22985.nbteditor.fabric.net.FabricClientChannel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NbteRuntime runtime = NbteRuntime.INSTANCE;
		runtime.initCommon();
		runtime.transport().client(new FabricClientChannel());
		FabricClientPlatform platform = new FabricClientPlatform();
		FabricClientHooks hooks = new FabricClientHooks(runtime::hooks, runtime::kernel, runtime::net, runtime.scheduler(), platform.world());
		hooks.register();
		runtime.chatHandler(hooks::chat);
		runtime.dropHandler(hooks::filesDropped);
		runtime.clickHandler(hooks::slotClicked);
		runtime.clientCommands(platform.commands()::handles);
		runtime.registries().preload();
		runtime.start(platform);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runtime.stop());
	}
}
