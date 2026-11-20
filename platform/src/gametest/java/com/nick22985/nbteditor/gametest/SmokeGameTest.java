package com.nick22985.nbteditor.gametest;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.compat.ModMenuEntry;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.module.ModuleInfo;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SmokeGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		if (kernel == null) {
			throw new AssertionError("Kernel didn't start");
		}
		for (ModuleInfo info : kernel.modules()) {
			System.out.println("[SmokeGameTest] module " + info.id() + " " + info.state() + (info.reason() == null ? "" : " " + info.reason()));
			if (!info.enabled() && info.module().enabledByDefault()) {
				throw new AssertionError("Module " + info.id() + " is " + info.state());
			}
		}
		context.setScreen(() -> new ModMenuEntry().getModConfigScreenFactory().create(null));
		context.waitTicks(5);
		//? if >=26.3 {
		boolean open = context.computeOnClient(client -> client.gui.screen() instanceof HostScreen);
		//?} else
		//boolean open = context.computeOnClient(client -> client.screen instanceof HostScreen);
		if (!open) {
			throw new AssertionError("Settings screen didn't open");
		}
		context.takeScreenshot("smoke-settings");
		context.setScreen(() -> null);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(20);
			context.takeScreenshot("smoke-world");
		}
	}
}
