package com.nick22985.nbteditor.fabric.mixin;

//? if >=1.21.4 {
import com.nick22985.nbteditor.fabric.util.McPickUtil;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class PickBlockMixin {
	@Inject(method = "handlePickItemFromBlock", at = @At("HEAD"), cancellable = true)
	private void nbtedit$pickBlock(BlockPos pos, boolean includeData, CallbackInfo info) {
		if (McPickUtil.pick(pos, includeData)) {
			info.cancel();
		}
	}
}
//?}
