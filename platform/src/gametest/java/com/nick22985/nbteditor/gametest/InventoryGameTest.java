package com.nick22985.nbteditor.gametest;

import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.modules.clientchest.ClientChest;
import com.nick22985.nbteditor.modules.clientchest.ClientChestOverviewScreen;
import com.nick22985.nbteditor.modules.clientchest.ClientChestScreen;
import com.nick22985.nbteditor.modules.lostitems.LostItemSource;
import com.nick22985.nbteditor.modules.lostitems.LostItems;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.ItemService;
import com.nick22985.nbteditor.target.WorldTargets;
import com.nick22985.nbteditor.ui.UiScreen;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class InventoryGameTest implements FabricClientGameTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final BlockPos CHEST = new BlockPos(2, -60, 2);
	private static final String[] ITEMS = {"minecraft:diamond_sword", "minecraft:golden_apple", "minecraft:elytra", "minecraft:totem_of_undying", "minecraft:nether_star", "minecraft:enchanted_book", "minecraft:shulker_box", "minecraft:trident", "minecraft:spyglass"};

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		ItemService items = kernel.platform().client().items();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("setblock 2 -60 2 minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:12},{Slot:4b,id:\"minecraft:emerald_block\",count:3},{Slot:13b,id:\"minecraft:netherite_ingot\",count:1}]}");
			world.getServer().runCommand("give @a minecraft:diamond_pickaxe");
			world.getServer().runCommand("give @a minecraft:oak_log 32");
			context.waitTicks(40);

			context.runOnClient(client -> kernel.services().get(ContainerService.class).open(kernel.services().get(WorldTargets.class).block(OVERWORLD, CHEST)));
			context.waitTicks(40);
			if (context.computeOnClient(this::ui) == null) {
				throw new AssertionError("Container screen didn't open");
			}
			context.takeScreenshot("m6-container");
			context.setScreen(() -> null);
			context.waitTicks(2);

			ClientChest chest = kernel.services().get(ClientChest.class);
			List<CompoundTag> page = new ArrayList<>();
			for (String id : ITEMS) {
				page.add(context.computeOnClient(client -> items.of(id, 1).tag()));
			}
			this.await(context, () -> chest.write(0, page));
			this.await(context, () -> chest.write(4, List.of(page.get(2), page.get(3))));
			chest.store().rename(0, "Treasure");
			chest.store().rename(9, "Builds");
			context.runOnClient(client -> chest.open(0));
			context.waitTicks(20);
			if (!(context.computeOnClient(this::ui) instanceof ClientChestScreen)) {
				throw new AssertionError("Client chest didn't open");
			}
			context.takeScreenshot("m6-client-chest");
			context.runOnClient(client -> ((ClientChestScreen) this.ui(client)).openOverview());
			context.waitTicks(20);
			if (!(context.computeOnClient(this::ui) instanceof ClientChestOverviewScreen)) {
				throw new AssertionError("Client chest overview didn't open");
			}
			context.takeScreenshot("m6-client-chest-overview");
			context.setScreen(() -> null);
			context.waitTicks(10);
			boolean saved = context.computeOnClient(client -> Files.isRegularFile(kernel.platform().game().gameDir().resolve("nbtedit/client_chest/page_0.dat")));
			if (!saved) {
				throw new AssertionError("Client chest page was not written to disk");
			}

			LostItems lost = kernel.services().get(LostItems.class);
			context.runOnClient(client -> {
				for (String id : ITEMS) {
					lost.history().add(LostItemSource.DELETED, items.of(id, 1));
				}
				lost.open(LostItemSource.DELETED);
			});
			context.waitTicks(20);
			context.takeScreenshot("m6-lost-items");
			context.setScreen(() -> null);
			context.waitTicks(2);
		}
	}

	private <T> T await(ClientGameTestContext context, Supplier<CompletableFuture<T>> task) {
		CompletableFuture<T> future = context.computeOnClient(client -> task.get());
		for (int i = 0; i < 400 && !future.isDone(); i++) {
			context.waitTicks(1);
		}
		if (!future.isDone()) {
			throw new AssertionError("Timed out waiting for a client chest operation");
		}
		return future.join();
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
