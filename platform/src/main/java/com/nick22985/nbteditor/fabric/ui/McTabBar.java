package com.nick22985.nbteditor.fabric.ui;

import com.nick22985.nbteditor.fabric.util.McInputUtil;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.SoundService;
import com.nick22985.nbteditor.ui.MouseButton;
import com.nick22985.nbteditor.ui.Rect;
import com.nick22985.nbteditor.ui.tab.InventoryTab;
import com.nick22985.nbteditor.ui.tab.TabPosition;
import com.nick22985.nbteditor.util.TabUtil;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import com.mojang.blaze3d.vertex.PoseStack;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McTabBar {
	//? if >=1.19.3 {
	private static final int WIDTH = 26;
	private static final int ITEM_X = 5;
	//?} else {
	/*private static final int WIDTH = 28;
	private static final int ITEM_X = 6;
	*///?}
	private static final int HEIGHT = 32;
	private static final int ITEM_TOP = 5;
	private static final int ITEM_BOTTOM = 11;
	//? if >=1.20.2 {
	private static final String TOP = "minecraft:container/creative_inventory/tab_top_unselected_2";
	private static final String BOTTOM = "minecraft:container/creative_inventory/tab_bottom_unselected_2";
	//?} else
	//private static final String TABS = "minecraft:textures/gui/container/creative_inventory/tabs.png";

	private final Screen screen;
	private final TabPosition position;
	private final List<InventoryTab> tabs;
	private final List<GameItem> icons = new ArrayList<>();
	private final List<Rect> rects;
	private final SoundService sounds;

	public McTabBar(Screen screen, TabPosition position, List<InventoryTab> tabs, SoundService sounds) {
		this.screen = screen;
		this.position = position;
		this.tabs = List.copyOf(tabs);
		this.sounds = sounds;
		for (InventoryTab tab : this.tabs) {
			this.icons.add(tab.icon().get());
		}
		this.rects = TabUtil.layout(position, screen.width, screen.height, this.tabs.size(), WIDTH, HEIGHT);
	}

	public void register() {
		//? if >=26.1 {
		ScreenEvents.afterExtract(this.screen).register((target, graphics, mouseX, mouseY, delta) -> this.render(graphics, mouseX, mouseY, delta));
		//?} else
		//ScreenEvents.afterRender(this.screen).register((target, graphics, mouseX, mouseY, delta) -> this.render(graphics, mouseX, mouseY, delta));
		//? if >=1.21.9 {
		ScreenMouseEvents.allowMouseClick(this.screen).register((target, event) -> !this.click(event.x(), event.y(), event.button()));
		//?} else
		//ScreenMouseEvents.allowMouseClick(this.screen).register((target, mouseX, mouseY, button) -> !this.click(mouseX, mouseY, button));
	}

	private boolean blocked() {
		return this.screen instanceof HostScreen host && host.ui().topOverlay() != null;
	}

	private int at(double mouseX, double mouseY) {
		for (int i = 0; i < this.rects.size(); i++) {
			if (this.rects.get(i).contains(mouseX, mouseY)) {
				return i;
			}
		}
		return -1;
	}

	private boolean click(double mouseX, double mouseY, int button) {
		if (this.blocked() || McInputUtil.button(button) != MouseButton.LEFT) {
			return false;
		}
		int index = this.at(mouseX, mouseY);
		if (index < 0) {
			return false;
		}
		this.sounds.click();
		this.tabs.get(index).onClick().run();
		return true;
	}

	//? if >=1.20 {
	private void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
	//?} else
	//private void render(PoseStack graphics, int mouseX, int mouseY, float delta) {
		if (this.blocked()) {
			return;
		}
		McCanvas canvas = new McCanvas(graphics, this.screen, delta);
		canvas.begin();
		try {
			canvas.nextLayer();
			boolean top = this.position.top();
			for (int i = 0; i < this.rects.size(); i++) {
				Rect rect = this.rects.get(i);
				//? if >=1.20.2 {
				canvas.sprite(top ? TOP : BOTTOM, rect.x(), rect.y(), WIDTH, HEIGHT);
				//?} else
				//canvas.texture(TABS, rect.x(), rect.y(), WIDTH, top ? 0 : 64, WIDTH, HEIGHT, 256, 256);
				canvas.item(this.icons.get(i), rect.x() + ITEM_X, rect.y() + (top ? ITEM_TOP : ITEM_BOTTOM), 1.0F, false);
			}
			int hovered = this.at(mouseX, mouseY);
			if (hovered >= 0) {
				canvas.tooltip(List.of(this.tabs.get(hovered).label()), mouseX, mouseY);
			}
		} finally {
			canvas.end();
		}
	}
}
