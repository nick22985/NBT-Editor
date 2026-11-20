package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.ui.Rect;
import com.nick22985.nbteditor.ui.tab.InventoryTabs;
import com.nick22985.nbteditor.ui.tab.TabPosition;
import com.nick22985.nbteditor.util.TabUtil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class TabGameTest implements FabricClientGameTest {
	private static final int TAB_WIDTH = 26;
	private static final int TAB_HEIGHT = 32;

	@Override
	@SuppressWarnings("unchecked")
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		Setting<TabPosition> position = (Setting<TabPosition>) kernel.config().section("creative-tabs").setting("position");
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(40);

			this.openInventory(context);
			this.expectScreen(context, "CreativeModeInventoryScreen");
			context.takeScreenshot("tabs-creative");
			this.clickTab(context, TabPosition.BOTTOM_LEFT, 2, 0);
			this.expectScreen(context, "ClientChestScreen");
			context.takeScreenshot("tabs-client-chest");
			this.clickTab(context, TabPosition.BOTTOM_LEFT, 2, 0);
			this.expectScreen(context, "CreativeModeInventoryScreen");
			this.clickTab(context, TabPosition.BOTTOM_LEFT, 2, 1);
			this.expectScreen(context, "ContainerScreen");
			context.takeScreenshot("tabs-ender-chest");
			this.close(context);

			context.runOnClient(client -> position.set(TabPosition.TOP_RIGHT));
			this.openInventory(context);
			context.takeScreenshot("tabs-top-right");
			this.clickTab(context, TabPosition.TOP_RIGHT, 2, 0);
			this.expectScreen(context, "ClientChestScreen");
			this.close(context);

			context.runOnClient(client -> position.set(TabPosition.HIDDEN));
			if (!kernel.services().get(InventoryTabs.class).position().hidden()) {
				throw new AssertionError("The hidden position didn't apply");
			}
			this.openInventory(context);
			this.clickTab(context, TabPosition.TOP_RIGHT, 2, 0);
			this.expectScreen(context, "CreativeModeInventoryScreen");
			context.takeScreenshot("tabs-hidden");
			this.close(context);
			context.runOnClient(client -> position.set(TabPosition.BOTTOM_LEFT));
		}
	}

	private void clickTab(ClientGameTestContext context, TabPosition position, int count, int index) {
		double[] center = context.computeOnClient(client -> {
			int width = client.getWindow().getGuiScaledWidth();
			int height = client.getWindow().getGuiScaledHeight();
			Rect rect = TabUtil.layout(position, width, height, count, TAB_WIDTH, TAB_HEIGHT).get(index);
			double scale = client.getWindow().getGuiScale();
			return new double[] {(rect.x() + rect.width() / 2.0) * scale, (rect.y() + rect.height() / 2.0) * scale};
		});
		context.getInput().setCursorPos(center[0], center[1]);
		context.waitTicks(3);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
	}

	private void openInventory(ClientGameTestContext context) {
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(10);
	}

	private void expectScreen(ClientGameTestContext context, String name) {
		String shown = context.computeOnClient(client -> {
			Object screen = this.screen(client);
			return screen instanceof HostScreen ? ((HostScreen) screen).ui().getClass().getSimpleName() : screen == null ? null : screen.getClass().getSimpleName();
		});
		if (!name.equals(shown)) {
			throw new AssertionError("Expected " + name + " but the screen was " + shown);
		}
	}

	private void close(ClientGameTestContext context) {
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private Object screen(Minecraft client) {
		//? if >=26.3 {
		return client.gui.screen();
		//?} else
		//return client.screen;
	}
}
