package com.nick22985.nbteditor.ui;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record MouseInput(double x, double y, MouseButton button, Modifiers modifiers, boolean doubleClick) {
	public int mouseX() {
		return (int) Math.floor(this.x);
	}

	public int mouseY() {
		return (int) Math.floor(this.y);
	}

	public boolean left() {
		return this.button == MouseButton.LEFT;
	}

	public boolean right() {
		return this.button == MouseButton.RIGHT;
	}

	public boolean middle() {
		return this.button == MouseButton.MIDDLE;
	}

	public MouseInput offset(double dx, double dy) {
		return new MouseInput(this.x + dx, this.y + dy, this.button, this.modifiers, this.doubleClick);
	}
}
