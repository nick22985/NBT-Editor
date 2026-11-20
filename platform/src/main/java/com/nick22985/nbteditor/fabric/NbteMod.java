package com.nick22985.nbteditor.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbteMod implements ModInitializer {
	@Override
	public void onInitialize() {
		NbteRuntime runtime = NbteRuntime.INSTANCE;
		runtime.initCommon();
		if (!runtime.game().isClient()) {
			runtime.start(null);
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> runtime.stop());
		}
	}
}
