package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.data.PackModel;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.modules.display.PackModelsScreen;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.ui.UiScreen;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ModelsGameTest implements FabricClientGameTest {
	private static final String WAND = "nbtedit_test:wand";

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("clear @a");
			context.waitTicks(40);
			context.runOnClient(client -> kernel.platform().client().commands().send("get models nbtedit_test"));
			context.waitTicks(15);
			if (!(context.computeOnClient(this::ui) instanceof PackModelsScreen)) {
				throw new AssertionError("/get models didn't open the model browser");
			}
			List<PackModel> shown = context.computeOnClient(client -> List.copyOf(((PackModelsScreen) this.ui(client)).shown()));
			List<PackModel> expected = List.of(new PackModel("minecraft:paper", "nbtedit_test:ruby", null, ""), new PackModel("minecraft:paper", WAND, null, ""), new PackModel("minecraft:paper", WAND, 1, "minecraft:item/blaze_rod"), new PackModel("minecraft:paper", WAND, 2, "minecraft:item/breeze_rod"));
			if (!expected.equals(shown)) {
				throw new AssertionError("The model browser showed " + shown);
			}
			double[] tile = context.computeOnClient(client -> this.tile(client, 3));
			context.getInput().setCursorPos(tile[0], tile[1]);
			context.waitTicks(2);
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(2);
			PackModel picked = context.computeOnClient(client -> {
				ModelEntry selected = ((PackModelsScreen) this.ui(client)).selected();
				return selected == null ? null : selected.model();
			});
			if (!expected.get(3).equals(picked)) {
				throw new AssertionError("Clicking a model tile selected " + picked);
			}
			context.takeScreenshot("models-search");
			context.runOnClient(client -> ((PackModelsScreen) this.ui(client)).view(true));
			context.waitTicks(2);
			context.takeScreenshot("models-list");
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(2);
			context.setScreen(() -> null);
			context.waitTicks(10);
			boolean taken = context.computeOnClient(client -> {
				for (int i = 0; i < 36; i++) {
					CompoundTag tag = kernel.platform().client().player().slot(i).tag();
					String text = tag.toString();
					if (text.contains(WAND) && text.contains("custom_model_data")) {
						return true;
					}
				}
				return false;
			});
			if (!taken) {
				throw new AssertionError("Taking a model from the browser didn't put it in the inventory");
			}
			context.getInput().resizeWindow(1920, 1080);
			context.runOnClient(client -> {
				client.options.guiScale().set(2);
				client.resizeGui();
				kernel.platform().client().commands().send("get models nbtedit_fx");
			});
			context.waitTicks(15);
			int count = context.computeOnClient(client -> {
				PackModelsScreen screen = (PackModelsScreen) this.ui(client);
				for (ModelEntry entry : screen.grid().order()) {
					if (entry.model().label().contains("enhancer")) {
						screen.select(entry);
					}
				}
				return screen.shown().size();
			});
			if (count != 9) {
				throw new AssertionError("The model browser showed " + count + " models in small folders");
			}
			context.waitTicks(2);
			context.takeScreenshot("models-blocks");
			context.setScreen(() -> null);
			context.runOnClient(client -> {
				client.options.guiScale().set(0);
				client.resizeGui();
			});
			context.getInput().resizeWindow(854, 480);
			context.waitTicks(2);
		}
	}

	private double[] tile(Minecraft client, int index) {
		PackModelsScreen screen = (PackModelsScreen) this.ui(client);
		double scale = client.getWindow().getGuiScale();
		ModelEntry entry = screen.grid().order().get(index);
		int[] position = screen.grid().tilePosition(entry);
		if (position == null) {
			throw new AssertionError("Couldn't find model tile " + index);
		}
		return new double[] {(position[0] + 0.5) * scale, (position[1] + 0.5) * scale};
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
