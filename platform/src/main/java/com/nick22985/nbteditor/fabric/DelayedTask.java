package com.nick22985.nbteditor.fabric;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DelayedTask {
	private final Runnable task;
	private int remaining;

	public DelayedTask(int ticks, Runnable task) {
		this.remaining = ticks;
		this.task = task;
	}

	public boolean tick() {
		return --this.remaining <= 0;
	}

	public Runnable task() {
		return this.task;
	}
}
