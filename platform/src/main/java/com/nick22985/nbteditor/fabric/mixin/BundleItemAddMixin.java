package com.nick22985.nbteditor.fabric.mixin;

//? if <1.20.5 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 ^/
@Mixin(BundleItem.class)
public abstract class BundleItemAddMixin {
	@WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;canFitInsideContainerItems()Z"))
	private static boolean nbtedit$addFits(Item item, Operation<Boolean> original) {
		return NbteRuntime.INSTANCE.slots().bundle(original.call(item));
	}
}
*///?}
