package com.nick22985.nbteditor.hook;

import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotCheckEvent {
	private final SlotRule rule;
	private final SlotAction action;
	private final UUID player;
	private final boolean server;
	private boolean allowed;

	public SlotCheckEvent(SlotRule rule, SlotAction action, UUID player, boolean server) {
		this.rule = rule;
		this.action = action;
		this.player = player;
		this.server = server;
	}

	public SlotRule rule() {
		return this.rule;
	}

	public SlotAction action() {
		return this.action;
	}

	public UUID player() {
		return this.player;
	}

	public boolean server() {
		return this.server;
	}

	public void allow() {
		this.allowed = true;
	}

	public boolean allowed() {
		return this.allowed;
	}
}
