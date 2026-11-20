package com.nick22985.nbteditor.fabric.mixin;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else
/*import net.minecraft.world.inventory.ClickType;*/
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin({AbstractContainerScreen.class, CreativeModeInventoryScreen.class})
public abstract class ContainerScreenClickMixin {
	//? if >=26.1 {
	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
	private void nbtedit$slotClicked(Slot slot, int slotId, int button, ContainerInput type, CallbackInfo info) {
		boolean pickup = type == ContainerInput.PICKUP;
	//?} else {
	/*@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", at = @At("HEAD"), cancellable = true)
	private void nbtedit$slotClicked(Slot slot, int slotId, int button, ClickType type, CallbackInfo info) {
		boolean pickup = type == ClickType.PICKUP;
	*///?}
		if (pickup && slot != null && NbteRuntime.INSTANCE.slotClicked((Screen) (Object) this, slot)) {
			info.cancel();
		}
	}
}
