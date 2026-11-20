package com.nick22985.nbteditor.ui;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class Overlay extends WidgetGroup {
	private UiScreen owner;

	public boolean dimBackground() {
		return true;
	}

	public boolean closeOnOutsideClick() {
		return false;
	}

	public boolean closeOnEscape() {
		return true;
	}

	public abstract void layout(int screenWidth, int screenHeight);

	void attachScreen(UiScreen screen) {
		this.owner = screen;
	}

	@Override
	public UiScreen screen() {
		return this.owner;
	}

	public void close() {
		if (this.owner != null) {
			this.owner.closeOverlay(this);
		}
	}

	public boolean isOpen() {
		return this.owner != null && this.owner.overlays().contains(this);
	}

	protected void onOpened() {
	}

	protected void onClosed() {
	}

	@Override
	public Widget widgetAt(double mouseX, double mouseY) {
		Widget found = super.widgetAt(mouseX, mouseY);
		return found == null ? this : found;
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!this.contains(input.x(), input.y()) && this.closeOnOutsideClick()) {
			this.close();
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		return true;
	}
}
