package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.platform.SystemService;
import java.net.URI;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
//? if >=26.3 {
import com.mojang.blaze3d.Blaze3D;
//?} else
//import net.minecraft.util.Util;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricSystemService implements SystemService {
	@Override
	public void openFile(Path path) {
		//? if >=26.3 {
		Blaze3D.openPath(path);
		//?} else
		//Util.getPlatform().openFile(path.toFile());
	}

	@Override
	public void openUrl(String url) {
		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException e) {
			return;
		}
		//? if >=26.3 {
		Blaze3D.openUri(uri);
		//?} else
		//Util.getPlatform().openUri(uri);
	}

	@Override
	public String clipboard() {
		return Minecraft.getInstance().keyboardHandler.getClipboard();
	}

	@Override
	public void setClipboard(String text) {
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
	}
}
