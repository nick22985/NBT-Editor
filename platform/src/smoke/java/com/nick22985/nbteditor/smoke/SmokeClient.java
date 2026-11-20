package com.nick22985.nbteditor.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SmokeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SmokeContext context = new SmokeContext();
		ClientTickEvents.END_CLIENT_TICK.register(client -> context.tick());
		Thread thread = new Thread(new SmokeRun(context), "nbtedit-smoke");
		thread.setDaemon(true);
		thread.start();
	}
}
