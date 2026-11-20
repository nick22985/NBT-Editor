package com.nick22985.nbteditor.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("hoveredSlot")
	Slot nbtedit$hoveredSlot();

	@Accessor("leftPos")
	int nbtedit$leftPos();

	@Accessor("topPos")
	int nbtedit$topPos();
}
