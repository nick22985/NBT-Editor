package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.hook.SlotRule;
import com.nick22985.nbteditor.platform.PlayerService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
//? if >=1.21.11 {
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
//?} else
/*import net.minecraft.world.inventory.HorseInventoryMenu;*/
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McSlotUtil {
	public static final int HOTBAR_SIZE = 9;
	public static final int MAIN_SIZE = 36;

	private McSlotUtil() {
	}

	public static int menuSlot(int slot) {
		if (slot >= 0 && slot < HOTBAR_SIZE) {
			return 36 + slot;
		}
		if (slot >= HOTBAR_SIZE && slot < MAIN_SIZE) {
			return slot;
		}
		return switch (slot) {
			case PlayerService.HEAD_SLOT -> 5;
			case PlayerService.CHEST_SLOT -> 6;
			case PlayerService.LEGS_SLOT -> 7;
			case PlayerService.FEET_SLOT -> 8;
			case PlayerService.OFFHAND_SLOT -> 45;
			default -> -1;
		};
	}

	public static int selected(Inventory inventory) {
		//? if >=1.21.5 {
		return inventory.getSelectedSlot();
		//?} else
		//return inventory.selected;
	}

	public static SlotRule rule(AbstractContainerMenu menu, Slot slot) {
		if (slot instanceof ShulkerBoxSlot) {
			return SlotRule.SHULKER_BOX;
		}
		if (slot instanceof FurnaceFuelSlot || slot instanceof FurnaceResultSlot) {
			return SlotRule.FURNACE;
		}
		if (slot.container instanceof Inventory) {
			int index = slot.getContainerSlot();
			return index >= MAIN_SIZE && index < MAIN_SIZE + 4 ? SlotRule.ARMOR : null;
		}
		if (menu instanceof BrewingStandMenu) {
			return SlotRule.BREWING_STAND;
		}
		//? if >=1.21.11 {
		if (menu instanceof AbstractMountInventoryMenu) {
		//?} else
		/*if (menu instanceof HorseInventoryMenu) {*/
			return SlotRule.MOUNT;
		}
		return null;
	}
}
