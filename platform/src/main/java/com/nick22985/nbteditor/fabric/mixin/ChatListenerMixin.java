package com.nick22985.nbteditor.fabric.mixin;

//? if >=1.19 && <1.19.3 {
/*import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 ^/
@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
	@Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
	private void nbtedit$systemMessage(Component message, boolean overlay, CallbackInfo info) {
		if (NbteRuntime.INSTANCE.chatReceived(message, overlay)) {
			info.cancel();
		}
	}
}
*///?}
