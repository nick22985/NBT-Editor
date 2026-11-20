package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.platform.ChatService;
import com.nick22985.nbteditor.text.Text;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricChatService implements ChatService {
	@Override
	public void message(Text text) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		Component component = McTextUtil.toMc(text);
		//? if >=26.1 {
		player.sendSystemMessage(component);
		//?} else
		//player.displayClientMessage(component, false);
	}

	@Override
	public void actionBar(Text text) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		Component component = McTextUtil.toMc(text);
		//? if >=26.1 {
		player.sendOverlayMessage(component);
		//?} else
		//player.displayClientMessage(component, true);
	}

	@Override
	public void longInput(BooleanSupplier enabled) {
		NbteRuntime.INSTANCE.longChat(enabled);
	}
}
