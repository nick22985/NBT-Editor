package com.nick22985.nbteditor.fabric.ui;

import com.nick22985.nbteditor.fabric.util.McInputUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.UiScreen;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
*///?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HostScreen extends Screen {
	private final UiScreen ui;
	private final Screen parent;
	private final McUiHost host;
	//? if <1.21.9
	//private final DoubleClickTracker clicks = new DoubleClickTracker();
	private boolean shown;

	public HostScreen(UiScreen ui, Screen parent, McUiHost host) {
		super(McTextUtil.toMc(ui.title()));
		this.ui = ui;
		this.parent = parent;
		this.host = host;
	}

	public UiScreen ui() {
		return this.ui;
	}

	public Screen parent() {
		return this.parent;
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		if (!this.ui.filesDropped(paths)) {
			super.onFilesDrop(paths);
		}
	}

	@Override
	protected void init() {
		if (this.ui.host() == null) {
			this.ui.attach(this.host, this.width, this.height);
		} else {
			this.ui.resize(this.width, this.height);
		}
		if (!this.shown) {
			this.shown = true;
			this.ui.onShown();
		}
	}

	@Override
	public void removed() {
		this.shown = false;
		this.ui.onRemoved();
	}

	@Override
	public void tick() {
		this.ui.tick();
	}

	@Override
	public boolean isPauseScreen() {
		return this.ui.pausesGame();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		this.host.close(this.ui);
	}

	//? if >=26.1 {
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.draw(new McCanvas(graphics, this, delta), mouseX, mouseY, delta);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.ui.vanillaBackground()) {
			super.extractBackground(graphics, mouseX, mouseY, delta);
		}
	}
	//?} else if >=1.20 {
	/*@Override
	public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		//? if <1.20.2 {
		/^if (this.ui.vanillaBackground()) {
			this.renderBackground(graphics);
		}
		^///?}
		super.render(graphics, mouseX, mouseY, delta);
		this.draw(new McCanvas(graphics, this, delta), mouseX, mouseY, delta);
	}

	//? if >=1.20.2 {
	@Override
	public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.ui.vanillaBackground()) {
			super.renderBackground(graphics, mouseX, mouseY, delta);
		}
	}
	//?}
	*///?} else {
	/*@Override
	public void render(PoseStack graphics, int mouseX, int mouseY, float delta) {
		if (this.ui.vanillaBackground()) {
			this.renderBackground(graphics);
		}
		super.render(graphics, mouseX, mouseY, delta);
		this.draw(new McCanvas(graphics, this, delta), mouseX, mouseY, delta);
	}

	public void legacyGradient(PoseStack graphics, int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
		fillGradient(graphics, x1, y1, x2, y2, colorTop, colorBottom);
	}

	public void legacyItemTooltip(PoseStack graphics, ItemStack stack, int x, int y) {
		this.renderTooltip(graphics, stack, x, y);
	}
	*///?}

	private void draw(McCanvas canvas, int mouseX, int mouseY, float delta) {
		canvas.begin();
		try {
			this.ui.renderScreen(canvas, mouseX, mouseY, delta);
		} finally {
			canvas.end();
		}
	}

	//? if >=1.21.9 {
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return this.ui.dispatchClick(new MouseInput(event.x(), event.y(), McInputUtil.button(event.button()), McInputUtil.modifiers(event.modifiers()), doubleClick));
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return this.ui.dispatchRelease(new MouseInput(event.x(), event.y(), McInputUtil.button(event.button()), McInputUtil.modifiers(event.modifiers()), false));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		return this.ui.dispatchDrag(new MouseInput(event.x(), event.y(), McInputUtil.button(event.button()), McInputUtil.modifiers(event.modifiers()), false), deltaX, deltaY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return this.ui.dispatchKey(new KeyInput(McInputUtil.toCore(event.key(), event.keycode()), event.keycode(), McInputUtil.modifiers(event.modifiers())));
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		return this.ui.dispatchKeyRelease(new KeyInput(McInputUtil.toCore(event.key(), event.keycode()), event.keycode(), McInputUtil.modifiers(event.modifiers())));
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return this.ui.dispatchChar(event.codepoint(), McInputUtil.currentModifiers());
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean doubleClick = this.clicks.click(mouseX, mouseY, button);
		return this.ui.dispatchClick(new MouseInput(mouseX, mouseY, McInputUtil.button(button), McInputUtil.currentModifiers(), doubleClick));
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return this.ui.dispatchRelease(new MouseInput(mouseX, mouseY, McInputUtil.button(button), McInputUtil.currentModifiers(), false));
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return this.ui.dispatchDrag(new MouseInput(mouseX, mouseY, McInputUtil.button(button), McInputUtil.currentModifiers(), false), deltaX, deltaY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return this.ui.dispatchKey(new KeyInput(McInputUtil.toCore(keyCode, scanCode), scanCode, McInputUtil.modifiers(modifiers)));
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return this.ui.dispatchKeyRelease(new KeyInput(McInputUtil.toCore(keyCode, scanCode), scanCode, McInputUtil.modifiers(modifiers)));
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		return this.ui.dispatchChar(chr, McInputUtil.modifiers(modifiers));
	}
	*///?}

	//? if >=1.20.2 {
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		return this.ui.dispatchScroll(mouseX, mouseY, deltaX, deltaY);
	}
	//?} else {
	/*@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return this.ui.dispatchScroll(mouseX, mouseY, 0.0, delta);
	}
	*///?}
}
