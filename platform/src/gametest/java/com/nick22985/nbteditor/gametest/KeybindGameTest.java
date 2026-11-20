package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.mixin.AbstractContainerScreenAccessor;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.modules.clientchest.ClientChest;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class KeybindGameTest implements FabricClientGameTest {
	private static final int SLOT_CENTER = 8;

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a hotbar.0 with minecraft:diamond_sword");
			world.getServer().runCommand("item replace entity @a hotbar.2 with minecraft:golden_apple 5");
			context.waitTicks(40);
			context.runOnClient(client -> {
				this.bind(client, "key.nbtedit.copy_nbt", InputConstants.KEY_J);
				this.bind(client, "key.nbtedit.client_chest", InputConstants.KEY_K);
			});

			this.openInventory(context);
			this.hover(context, 0);
			context.getInput().pressKey(InputConstants.KEY_N);
			context.waitTicks(10);
			this.expectScreen(context, "EditorScreen");
			context.takeScreenshot("keybind-open-hovered");
			this.close(context);

			this.openInventory(context);
			this.hover(context, 2);
			context.getInput().pressKey(InputConstants.KEY_J);
			context.waitTicks(5);
			String clipboard = context.computeOnClient(client -> kernel.platform().client().system().clipboard());
			if (clipboard == null || !clipboard.contains("golden_apple")) {
				throw new AssertionError("Copy NBT put " + clipboard + " on the clipboard");
			}
			context.getInput().pressKey(InputConstants.KEY_K);
			context.waitTicks(10);
			context.takeScreenshot("keybind-stash");
			ClientChest chest = kernel.services().get(ClientChest.class);
			List<CompoundTag> page = this.await(context, chest.read(chest.lastPage()));
			if (page.stream().noneMatch(tag -> "minecraft:golden_apple".equals(ItemStackUtil.id(tag)))) {
				throw new AssertionError("Clone to client chest didn't store the apple");
			}
			this.close(context);

			world.getServer().runCommand("item replace entity @a hotbar.3 with minecraft:enchanted_book[stored_enchantments={\"minecraft:sharpness\":5}]");
			world.getServer().runCommand("item replace entity @a hotbar.4 with minecraft:dirt 3");
			context.waitTicks(20);
			this.openInventory(context);
			this.hover(context, 0);
			context.takeScreenshot("keybind-hints");
			context.getInput().pressKey(InputConstants.KEY_SPACE);
			context.waitTicks(10);
			this.expectScreen(context, "EditorScreen");
			this.close(context);

			this.openInventory(context);
			this.hover(context, 4);
			context.getInput().pressKey(InputConstants.KEY_DELETE);
			context.waitTicks(10);
			if (context.computeOnClient(client -> !client.player.getInventory().getItem(4).isEmpty())) {
				throw new AssertionError("Delete didn't clear the hovered slot");
			}

			this.hover(context, 3);
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(5);
			this.hover(context, 0);
			context.getInput().holdControl();
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.getInput().releaseControl();
			context.waitTicks(10);
			String sword = context.computeOnClient(client -> client.player.getInventory().getItem(0).getEnchantments().toString());
			boolean carried = context.computeOnClient(client -> !client.player.containerMenu.getCarried().isEmpty());
			if (!sword.contains("sharpness") || carried) {
				throw new AssertionError("Ctrl + Click with a book left the sword with " + sword + " and carried=" + carried);
			}
			this.close(context);

			context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
			context.waitTicks(2);
			context.getInput().pressKey(InputConstants.KEY_N);
			context.waitTicks(20);
			this.expectScreen(context, "EditorScreen");
			this.close(context);
		}
	}

	private void bind(Minecraft client, String name, int key) {
		for (KeyMapping mapping : client.options.keyMappings) {
			if (mapping.getName().equals(name)) {
				mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(key));
			}
		}
		KeyMapping.resetMapping();
	}

	private void openInventory(ClientGameTestContext context) {
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
	}

	private void hover(ClientGameTestContext context, int inventorySlot) {
		double[] position = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) this.screen(client);
			double scale = client.getWindow().getGuiScale();
			int left = ((AbstractContainerScreenAccessor) screen).nbtedit$leftPos();
			int top = ((AbstractContainerScreenAccessor) screen).nbtedit$topPos();
			for (Slot slot : screen.getMenu().slots) {
				if (slot.container == client.player.getInventory() && slot.getContainerSlot() == inventorySlot) {
					return new double[] {(left + slot.x + SLOT_CENTER) * scale, (top + slot.y + SLOT_CENTER) * scale};
				}
			}
			return null;
		});
		if (position == null) {
			throw new AssertionError("No inventory slot " + inventorySlot);
		}
		context.getInput().setCursorPos(position[0], position[1]);
		context.waitTicks(3);
		boolean hovered = context.computeOnClient(client -> {
			Slot slot = ((AbstractContainerScreenAccessor) this.screen(client)).nbtedit$hoveredSlot();
			return slot != null && slot.getContainerSlot() == inventorySlot;
		});
		if (!hovered) {
			throw new AssertionError("Cursor isn't over inventory slot " + inventorySlot);
		}
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

	private <T> T await(ClientGameTestContext context, CompletableFuture<T> future) {
		for (int i = 0; i < 400 && !future.isDone(); i++) {
			context.waitTicks(1);
		}
		if (!future.isDone()) {
			throw new AssertionError("Timed out waiting for the client chest");
		}
		return future.join();
	}

	private Object screen(Minecraft client) {
		//? if >=26.3 {
		return client.gui.screen();
		//?} else
		//return client.screen;
	}
}
