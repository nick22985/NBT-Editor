package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.modules.headdb.Head;
import com.nick22985.nbteditor.modules.headdb.HeadCategory;
import com.nick22985.nbteditor.modules.headdb.HeadDatabase;
import com.nick22985.nbteditor.modules.headdb.HeadDbScreen;
import com.nick22985.nbteditor.modules.headdb.HeadDbState;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.inventory.SlotRef;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SkullUtil;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class HeadDbGameTest implements FabricClientGameTest {
	private static final int DOWNLOAD_TICKS = 2400;

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		HeadDatabase database = kernel.services().get(HeadDatabase.class);
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(40);
			context.runOnClient(client -> kernel.platform().client().commands().send("get hdb dragon"));
			context.waitTicks(15);
			if (!(context.computeOnClient(this::ui) instanceof HeadDbScreen)) {
				throw new AssertionError("/get hdb didn't open the head database");
			}
			for (int i = 0; i < DOWNLOAD_TICKS && (database.state() == HeadDbState.LOADING || database.state() == HeadDbState.IDLE); i++) {
				context.waitTicks(1);
			}
			context.waitTicks(10);
			if (database.state() == HeadDbState.FAILED) {
				context.takeScreenshot("headdb-offline");
				System.out.println("Head database couldn't be downloaded, skipping the rest of the head database test");
				return;
			}
			Head first = context.computeOnClient(client -> {
				HeadDbScreen screen = (HeadDbScreen) this.ui(client);
				return screen.shown().isEmpty() ? null : screen.shown().get(0);
			});
			if (first == null || !first.name().toLowerCase().contains("dragon") && first.tags().stream().noneMatch(tag -> tag.toLowerCase().contains("dragon"))) {
				throw new AssertionError("Searching for dragon showed " + first);
			}
			context.takeScreenshot("headdb-search");

			double[] slot = context.computeOnClient(client -> this.firstSlot(client));
			context.getInput().setCursorPos(slot[0], slot[1]);
			context.waitTicks(2);
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_MIDDLE);
			context.waitTicks(2);
			String clipboard = context.computeOnClient(client -> kernel.platform().client().system().clipboard());
			if (!first.value().equals(clipboard)) {
				throw new AssertionError("Middle-click copied " + clipboard);
			}
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(2);
			if (!database.favorite(first)) {
				throw new AssertionError("Right-click didn't favorite " + first.name());
			}
			context.waitTicks(10);
			context.takeScreenshot("headdb-favorite");
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(2);
			if (database.favorite(first)) {
				throw new AssertionError("Right-click didn't remove " + first.name() + " from favorites");
			}
			context.setScreen(() -> null);
			context.waitTicks(2);
			world.getServer().runCommand("clear @a");
			context.waitTicks(5);
			context.runOnClient(client -> kernel.platform().client().commands().send("get hdb all animals search cat"));
			context.waitTicks(30);
			boolean boxed = context.computeOnClient(client -> {
				CompoundTag held = kernel.platform().client().player().slot(0).tag();
				return "minecraft:shulker_box".equals(ItemStackUtil.id(held)) && held.toString().contains("minecraft:player_head");
			});
			if (!boxed) {
				throw new AssertionError("/get hdb all didn't give a shulker box of heads");
			}
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("setblock 4 -60 2 minecraft:player_head{profile:{id:[I;1,2,3,4],properties:[{name:\"textures\",value:\"" + first.value() + "\"}]},custom_name:\"\"}");
			context.waitTicks(20);
			context.runOnClient(client -> client.gameMode.handlePickItemFromBlock(new BlockPos(4, -60, 2), false));
			context.waitTicks(20);
			String picked = context.computeOnClient(client -> {
				Component name = client.player.getMainHandItem().get(DataComponents.CUSTOM_NAME);
				return name == null ? "" : name.getString();
			});
			String skin = SkullUtil.skinId(first.value());
			if (database.heads(HeadCategory.ALL).stream().noneMatch(head -> head.name().equals(picked) && SkullUtil.skinId(head.value()).equals(skin))) {
				throw new AssertionError("Picking a database head named it '" + picked + "' instead of " + first.name());
			}
			CompletableFuture<Optional<Head>> lookup = context.computeOnClient(client -> database.player("jeb_"));
			for (int i = 0; i < 600 && !lookup.isDone(); i++) {
				context.waitTicks(1);
			}
			Optional<Head> player = lookup.isDone() && !lookup.isCompletedExceptionally() ? lookup.join() : null;
			if (player == null) {
				System.out.println("Couldn't reach Mojang, skipping the player head lookup");
			} else if (player.isEmpty() || !database.heads(HeadCategory.PLAYERS).contains(player.get())) {
				throw new AssertionError("Looking up jeb_ gave " + player);
			}
		}
	}

	private double[] firstSlot(Minecraft client) {
		HeadDbScreen screen = (HeadDbScreen) this.ui(client);
		double scale = client.getWindow().getGuiScale();
		for (int y = 0; y < screen.height(); y++) {
			for (int x = 0; x < screen.width(); x++) {
				SlotRef ref = screen.slotAt(x + 0.5, y + 0.5);
				if (ref != null && ref.section() == screen.list() && ref.index() == 0) {
					return new double[] {(x + 4) * scale, (y + 4) * scale};
				}
			}
		}
		throw new AssertionError("Couldn't find the first head slot");
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
