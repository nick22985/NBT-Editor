package com.nick22985.nbteditor.fabric.ui;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DoubleClickTracker {
	private static final long WINDOW_MILLIS = 250L;
	private static final double MAX_DISTANCE = 4.0;

	private long lastTime;
	private int lastButton = -1;
	private double lastX;
	private double lastY;

	public boolean click(double x, double y, int button) {
		long now = System.currentTimeMillis();
		boolean doubleClick = button == this.lastButton && now - this.lastTime < WINDOW_MILLIS && Math.abs(x - this.lastX) <= MAX_DISTANCE && Math.abs(y - this.lastY) <= MAX_DISTANCE;
		this.lastTime = doubleClick ? 0L : now;
		this.lastButton = button;
		this.lastX = x;
		this.lastY = y;
		return doubleClick;
	}
}
