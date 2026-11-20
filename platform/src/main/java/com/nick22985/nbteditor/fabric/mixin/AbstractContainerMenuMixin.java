package com.nick22985.nbteditor.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	@Inject(method = "clicked", at = @At("HEAD"))
	private void nbtedit$beginClick(CallbackInfo info, @Local(argsOnly = true) Player player) {
		NbteRuntime.INSTANCE.slots().begin((AbstractContainerMenu) (Object) this, player);
	}

	@Inject(method = "clicked", at = @At("RETURN"))
	private void nbtedit$endClick(CallbackInfo info) {
		NbteRuntime.INSTANCE.slots().end();
	}

	@WrapOperation(method = {"doClick", "moveItemStackTo"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean nbtedit$mayPlace(Slot slot, ItemStack stack, Operation<Boolean> original) {
		return NbteRuntime.INSTANCE.slots().mayPlace(slot, original.call(slot, stack));
	}

	@WrapOperation(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPickup(Lnet/minecraft/world/entity/player/Player;)Z"))
	private boolean nbtedit$mayPickup(Slot slot, Player player, Operation<Boolean> original) {
		return NbteRuntime.INSTANCE.slots().mayPickup(slot, original.call(slot, player));
	}
}
