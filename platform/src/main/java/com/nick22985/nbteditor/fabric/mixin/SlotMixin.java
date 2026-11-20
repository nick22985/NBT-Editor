package com.nick22985.nbteditor.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@WrapOperation(method = {"safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", "allowModification"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean nbtedit$mayPlace(Slot slot, ItemStack stack, Operation<Boolean> original) {
		return NbteRuntime.INSTANCE.slots().mayPlace(slot, original.call(slot, stack));
	}

	@WrapOperation(method = {"tryRemove", "allowModification"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPickup(Lnet/minecraft/world/entity/player/Player;)Z"))
	private boolean nbtedit$mayPickup(Slot slot, Player player, Operation<Boolean> original) {
		return NbteRuntime.INSTANCE.slots().mayPickup(slot, original.call(slot, player));
	}
}
