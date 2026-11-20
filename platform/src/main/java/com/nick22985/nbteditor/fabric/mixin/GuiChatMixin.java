package com.nick22985.nbteditor.fabric.mixin;

//? if <1.19 {
/*import com.nick22985.nbteditor.fabric.NbteRuntime;
import java.util.UUID;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 ^/
@Mixin(Gui.class)
public abstract class GuiChatMixin {
	@Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
	private void nbtedit$handleChat(ChatType type, Component message, UUID sender, CallbackInfo info) {
		if (type != ChatType.CHAT && NbteRuntime.INSTANCE.chatReceived(message, type == ChatType.GAME_INFO)) {
			info.cancel();
		}
	}
}
*///?}
