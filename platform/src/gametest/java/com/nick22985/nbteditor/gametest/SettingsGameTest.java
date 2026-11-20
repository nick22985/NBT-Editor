package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.settings.SettingsScreen;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.TextField;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SettingsGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		Setting<?> pick = kernel.config().section("targets-world").setting("client_pick");
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			context.runOnClient(client -> kernel.platform().client().commands().send("nbt config"));
			context.waitTicks(15);
			if (!(context.computeOnClient(this::ui) instanceof SettingsScreen)) {
				throw new AssertionError("/nbt config didn't open the settings");
			}
			this.click(context, this.find(context, TextField.class));
			context.getInput().typeChars("Pick block keeps");
			context.waitTicks(5);
			Object before = pick.get();
			double[] box = this.find(context, Checkbox.class);
			context.getInput().setCursorPos(0, 0);
			context.waitTicks(2);
			context.takeScreenshot("settings-before");
			this.click(context, box);
			context.waitTicks(5);
			context.takeScreenshot("settings-toggle");
			if (pick.get().equals(before)) {
				throw new AssertionError("Clicking the Pick block keeps data checkbox didn't change it from " + before);
			}
			boolean shown = context.computeOnClient(client -> ((Checkbox) this.ui(client).widgetAt(box[2], box[3])).checked());
			if (!pick.get().equals(shown)) {
				throw new AssertionError("The checkbox shows " + shown + " but the setting is " + pick.get());
			}
			this.click(context, box);
			context.waitTicks(5);
			if (!pick.get().equals(before)) {
				throw new AssertionError("Clicking the checkbox again didn't turn it back to " + before);
			}
			context.setScreen(() -> null);
			context.waitTicks(2);
		}
	}

	private void click(ClientGameTestContext context, double[] at) {
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	private double[] find(ClientGameTestContext context, Class<? extends Widget> type) {
		return context.computeOnClient(client -> {
			UiScreen screen = this.ui(client);
			double scale = client.getWindow().getGuiScale();
			for (int y = 0; y < screen.height(); y++) {
				for (int x = 0; x < screen.width(); x++) {
					if (type.isInstance(screen.widgetAt(x + 3.5, y + 3.5))) {
						return new double[] {(x + 3.5) * scale, (y + 3.5) * scale, x + 3.5, y + 3.5};
					}
				}
			}
			throw new AssertionError("Couldn't find a " + type.getSimpleName() + " on the settings screen");
		});
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
