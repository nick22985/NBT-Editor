package com.nick22985.nbteditor.fabric.mixin;

//? if >=1.21.2 {
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(BundleContents.class)
public abstract class BundleContentsMixin {
	@ModifyReturnValue(method = "canItemBeInBundle", at = @At("RETURN"))
	private static boolean nbtedit$fits(boolean original, @Local(argsOnly = true) ItemStack stack) {
		return original || !stack.isEmpty() && NbteRuntime.INSTANCE.slots().bundle(false);
	}
}
//?}
