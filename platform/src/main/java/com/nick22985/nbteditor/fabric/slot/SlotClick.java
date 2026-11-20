package com.nick22985.nbteditor.fabric.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record SlotClick(AbstractContainerMenu menu, Player player) {
}
