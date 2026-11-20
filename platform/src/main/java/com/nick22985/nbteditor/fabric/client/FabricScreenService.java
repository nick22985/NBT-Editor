package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.fabric.ui.McUiHost;
import com.nick22985.nbteditor.fabric.util.McClientUtil;
import com.nick22985.nbteditor.fabric.util.McScreenUtil;
import com.nick22985.nbteditor.platform.ScreenService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricScreenService implements ScreenService {
	private final McUiHost host;

	public FabricScreenService(McUiHost host) {
		this.host = host;
	}

	@Override
	public UiHost host() {
		return this.host;
	}

	@Override
	public void open(UiScreen screen) {
		this.host.open(screen);
	}

	@Override
	public void closeAll() {
		Minecraft.getInstance().execute(() -> McClientUtil.setScreen(null));
	}

	@Override
	public void openInventory() {
		Minecraft.getInstance().execute(() -> {
			Player player = McClientUtil.mc().player;
			if (player != null) {
				McClientUtil.setScreen(new InventoryScreen(player));
			}
		});
	}

	@Override
	public boolean anyOpen() {
		return McClientUtil.screen() != null;
	}

	@Override
	public UiScreen current() {
		return McClientUtil.screen() instanceof HostScreen host ? host.ui() : null;
	}

	@Override
	public HoveredItem hovered() {
		return McScreenUtil.hovered(McClientUtil.screen());
	}

	@Override
	public void toast(Text title, Text message) {
		this.host.toast(title, message);
	}
}
