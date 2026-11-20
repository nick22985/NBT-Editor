package com.nick22985.nbteditor.fabric.mixin;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.client.gui.screens.ChatScreen;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenInputMixin {
	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V"), index = 0)
	private int nbtedit$maxLength(int length) {
		return NbteRuntime.INSTANCE.longChat() ? Integer.MAX_VALUE : length;
	}

	//? if >=1.19 {
	@Inject(method = "normalizeChatMessage", at = @At("HEAD"), cancellable = true)
	private void nbtedit$normalize(String message, CallbackInfoReturnable<String> info) {
		String trimmed = message.trim();
		if (NbteRuntime.INSTANCE.longChat() && NbteRuntime.INSTANCE.clientCommand(trimmed)) {
			info.setReturnValue(StringUtils.normalizeSpace(trimmed));
		}
	}
	//?}
}
