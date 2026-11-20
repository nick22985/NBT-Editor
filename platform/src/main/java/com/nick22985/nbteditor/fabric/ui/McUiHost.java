package com.nick22985.nbteditor.fabric.ui;

import com.nick22985.nbteditor.fabric.util.McClientUtil;
import com.nick22985.nbteditor.fabric.util.McInputUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.platform.SoundService;
import com.nick22985.nbteditor.platform.SystemService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McUiHost extends McTextMetrics implements UiHost {
	private final SoundService sounds;
	private final SystemService system;

	public McUiHost(SoundService sounds, SystemService system) {
		this.sounds = sounds;
		this.system = system;
	}

	private static void onClient(Runnable task) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.isSameThread()) {
			task.run();
		} else {
			mc.execute(task);
		}
	}

	public HostScreen hostOf(UiScreen screen) {
		Screen current = McClientUtil.screen();
		while (current instanceof HostScreen host) {
			if (host.ui() == screen) {
				return host;
			}
			current = host.parent();
		}
		return null;
	}

	@Override
	public int screenWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	@Override
	public int screenHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}

	@Override
	public String clipboard() {
		return this.system.clipboard();
	}

	@Override
	public void setClipboard(String text) {
		this.system.setClipboard(text);
	}

	@Override
	public void playClick() {
		this.sounds.click();
	}

	@Override
	public void open(UiScreen screen) {
		onClient(() -> McClientUtil.setScreen(new HostScreen(screen, McClientUtil.screen(), this)));
	}

	@Override
	public void replace(UiScreen screen) {
		onClient(() -> {
			Screen current = McClientUtil.screen();
			Screen parent = current instanceof HostScreen host ? host.parent() : current;
			McClientUtil.setScreen(new HostScreen(screen, parent, this));
		});
	}

	@Override
	public void close(UiScreen screen) {
		onClient(() -> {
			HostScreen host = this.hostOf(screen);
			if (host != null) {
				McClientUtil.setScreen(host.parent());
			}
		});
	}

	@Override
	public boolean isKeyDown(int key) {
		return McInputUtil.isDown(key);
	}

	@Override
	public Modifiers modifiers() {
		return McInputUtil.currentModifiers();
	}

	@Override
	public long millis() {
		return System.currentTimeMillis();
	}

	@Override
	public String translate(String key, Object... args) {
		return I18n.get(key, args);
	}

	@Override
	public boolean hasTranslation(String key) {
		return Language.getInstance().has(key);
	}

	@Override
	public void toast(Text title, Text message) {
		onClient(() -> McClientUtil.toast(McTextUtil.toMc(title), McTextUtil.toMc(message)));
	}
}
