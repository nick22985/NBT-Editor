package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.text.Text;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class Widget {
	private WidgetGroup parent;
	protected int x;
	protected int y;
	protected int width;
	protected int height;
	private boolean visible = true;
	private boolean enabled = true;
	private Supplier<List<Text>> tooltip;

	public Widget bounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.onLayout();
		return this;
	}

	public Widget position(int x, int y) {
		return this.bounds(x, y, this.width, this.height);
	}

	public Widget size(int width, int height) {
		return this.bounds(this.x, this.y, width, height);
	}

	protected void onLayout() {
	}

	public int x() {
		return this.x;
	}

	public int y() {
		return this.y;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}

	public int right() {
		return this.x + this.width;
	}

	public int bottom() {
		return this.y + this.height;
	}

	public Rect rect() {
		return new Rect(this.x, this.y, this.width, this.height);
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
	}

	public boolean visible() {
		return this.visible;
	}

	public Widget visible(boolean visible) {
		this.visible = visible;
		return this;
	}

	public boolean enabled() {
		return this.enabled;
	}

	public Widget enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public boolean interactive() {
		return this.visible && this.enabled && (this.parent == null || this.parent.interactive());
	}

	public WidgetGroup parent() {
		return this.parent;
	}

	void attachTo(WidgetGroup parent) {
		this.parent = parent;
	}

	public UiScreen screen() {
		return this.parent == null ? null : this.parent.screen();
	}

	public UiHost host() {
		UiScreen screen = this.screen();
		return screen == null ? null : screen.host();
	}

	public abstract void render(Canvas canvas, int mouseX, int mouseY, float delta);

	public Widget widgetAt(double mouseX, double mouseY) {
		return this.visible && this.contains(mouseX, mouseY) ? this : null;
	}

	public boolean mouseClicked(MouseInput input) {
		return false;
	}

	public boolean mouseReleased(MouseInput input) {
		return false;
	}

	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		return false;
	}

	public boolean keyPressed(KeyInput input) {
		return false;
	}

	public boolean keyReleased(KeyInput input) {
		return false;
	}

	public boolean charTyped(int codepoint, Modifiers modifiers) {
		return false;
	}

	public boolean acceptsText() {
		return false;
	}

	public boolean focusable() {
		return false;
	}

	public boolean focused() {
		UiScreen screen = this.screen();
		return screen != null && screen.focusedWidget() == this;
	}

	public void focus() {
		UiScreen screen = this.screen();
		if (screen != null) {
			screen.setFocus(this);
		}
	}

	protected void focusChanged(boolean focused) {
	}

	public void tick() {
	}

	public Widget tooltip(Text... lines) {
		List<Text> copy = List.of(lines);
		this.tooltip = copy.isEmpty() ? null : () -> copy;
		return this;
	}

	public Widget tooltip(Supplier<List<Text>> tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	public List<Text> tooltip(int mouseX, int mouseY) {
		return this.tooltip == null ? null : this.tooltip.get();
	}

	protected void playClick() {
		UiHost host = this.host();
		if (host != null) {
			host.playClick();
		}
	}
}
