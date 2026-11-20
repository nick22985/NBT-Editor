package com.nick22985.nbteditor.ui;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record Rect(int x, int y, int width, int height) {
	public static final Rect EMPTY = new Rect(0, 0, 0, 0);

	public int right() {
		return this.x + this.width;
	}

	public int bottom() {
		return this.y + this.height;
	}

	public boolean contains(double px, double py) {
		return px >= this.x && py >= this.y && px < this.x + this.width && py < this.y + this.height;
	}

	public Rect inset(int amount) {
		return new Rect(this.x + amount, this.y + amount, Math.max(0, this.width - amount * 2), Math.max(0, this.height - amount * 2));
	}
}
