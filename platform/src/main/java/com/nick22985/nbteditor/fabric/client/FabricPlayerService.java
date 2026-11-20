package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.util.McPermissionUtil;
import com.nick22985.nbteditor.fabric.util.McSlotUtil;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.platform.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricPlayerService implements PlayerService {
	private LocalPlayer player() {
		return Minecraft.getInstance().player;
	}

	@Override
	public boolean present() {
		return this.player() != null;
	}

	@Override
	public String name() {
		LocalPlayer player = this.player();
		return player == null ? Minecraft.getInstance().getUser().getName() : player.getName().getString();
	}

	@Override
	public UUID uuid() {
		LocalPlayer player = this.player();
		return player == null ? null : player.getUUID();
	}

	@Override
	public int networkId() {
		LocalPlayer player = this.player();
		return player == null ? -1 : player.getId();
	}

	@Override
	public Position position() {
		LocalPlayer player = this.player();
		return player == null ? Position.ORIGIN : new Position(player.getX(), player.getY(), player.getZ());
	}

	@Override
	public boolean creative() {
		LocalPlayer player = this.player();
		return player != null && player.isCreative();
	}

	@Override
	public int permissionLevel() {
		return McPermissionUtil.level(this.player());
	}

	@Override
	public int selectedSlot() {
		LocalPlayer player = this.player();
		return player == null ? 0 : McSlotUtil.selected(player.getInventory());
	}

	@Override
	public GameItem slot(int slot) {
		LocalPlayer player = this.player();
		if (player == null || slot < 0 || slot > OFFHAND_SLOT) {
			return FabricGameItem.of(ItemStack.EMPTY);
		}
		return FabricGameItem.of(player.getInventory().getItem(slot));
	}

	@Override
	public GameItem held(Hand hand) {
		return this.slot(this.heldSlot(hand));
	}

	@Override
	public int heldSlot(Hand hand) {
		return hand == Hand.OFF ? OFFHAND_SLOT : this.selectedSlot();
	}

	@Override
	public boolean setSlot(int slot, GameItem item) {
		LocalPlayer player = this.player();
		Minecraft mc = Minecraft.getInstance();
		int menuSlot = McSlotUtil.menuSlot(slot);
		if (player == null || mc.gameMode == null || !player.isCreative() || menuSlot < 0) {
			return false;
		}
		ItemStack stack = FabricGameItem.stackOf(item);
		player.getInventory().setItem(slot, stack.copy());
		mc.gameMode.handleCreativeModeItemAdd(stack, menuSlot);
		player.inventoryMenu.broadcastChanges();
		return true;
	}

	@Override
	public GameItem cursor() {
		LocalPlayer player = this.player();
		return player == null ? FabricGameItem.of(ItemStack.EMPTY) : FabricGameItem.of(player.containerMenu.getCarried());
	}

	@Override
	public void setCursor(GameItem item) {
		LocalPlayer player = this.player();
		if (player != null) {
			player.containerMenu.setCarried(FabricGameItem.stackOf(item));
		}
	}

	@Override
	public boolean give(GameItem item) {
		LocalPlayer player = this.player();
		if (player == null) {
			return false;
		}
		Inventory inventory = player.getInventory();
		int selected = McSlotUtil.selected(inventory);
		int target = inventory.getItem(selected).isEmpty() ? selected : inventory.getFreeSlot();
		return target >= 0 && this.setSlot(target, item);
	}

	@Override
	public int inventoryChanges() {
		LocalPlayer player = this.player();
		return player == null ? 0 : player.getInventory().getTimesChanged();
	}

	@Override
	public List<GameItem> menuItems() {
		LocalPlayer player = this.player();
		List<GameItem> items = new ArrayList<>();
		if (player == null) {
			return items;
		}
		AbstractContainerMenu menu = player.containerMenu;
		if (menu == null || menu == player.inventoryMenu) {
			return items;
		}
		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof Inventory) && slot.hasItem()) {
				items.add(FabricGameItem.of(slot.getItem()));
			}
		}
		return items;
	}
}
