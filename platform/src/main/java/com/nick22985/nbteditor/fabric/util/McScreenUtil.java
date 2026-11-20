package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.mixin.AbstractContainerScreenAccessor;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.ui.HoveredItem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McScreenUtil {
	private McScreenUtil() {
	}

	public static HoveredItem hovered(Screen screen) {
		if (screen instanceof HostScreen) {
			return ((HostScreen) screen).ui().hoveredItem();
		}
		if (screen instanceof AbstractContainerScreen) {
			return hovered(((AbstractContainerScreenAccessor) screen).nbtedit$hoveredSlot());
		}
		return null;
	}

	public static HoveredItem hovered(Slot slot) {
		if (slot == null || !slot.hasItem()) {
			return null;
		}
		Player player = McClientUtil.mc().player;
		boolean own = player != null && slot.container == player.getInventory();
		return new HoveredItem(FabricGameItem.of(slot.getItem()), null, own ? slot.getContainerSlot() : HoveredItem.NO_SLOT);
	}
}
