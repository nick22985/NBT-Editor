package com.nick22985.nbteditor.fabric.mixin;

//? if <1.21.2 {
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
public abstract class BundleItemMixin {
	@WrapOperation(method = "overrideStackedOnOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;canFitInsideContainerItems()Z"))
	private boolean nbtedit$fits(Item item, Operation<Boolean> original) {
		return NbteRuntime.INSTANCE.slots().bundle(original.call(item));
	}
}
*///?}
