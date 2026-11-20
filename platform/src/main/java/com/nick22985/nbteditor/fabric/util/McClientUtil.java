package com.nick22985.nbteditor.fabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McClientUtil {
	private McClientUtil() {
	}

	public static Minecraft mc() {
		return Minecraft.getInstance();
	}

	public static Screen screen() {
		//? if >=26.2 {
		return mc().gui.screen();
		//?} else
		//return mc().screen;
	}

	public static void setScreen(Screen screen) {
		//? if >=26.2 {
		mc().gui.setScreen(screen);
		//?} else
		//mc().setScreen(screen);
	}

	public static void toast(Component title, Component message) {
		//? if >=26.2 {
		SystemToast.add(mc().gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
		//?} else if >=1.21.2 {
		/*SystemToast.add(mc().getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
		*///?} else if >=1.20.3 {
		/*SystemToast.add(mc().getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
		*///?} else
		//SystemToast.add(mc().getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT, title, message);
	}
}
