package com.nick22985.nbteditor.modules.vanilla;

import com.nick22985.nbteditor.text.Text;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class QueuedCommand {
	private final String command;
	private final Set<String> successKeys;
	private final Set<String> harmlessKeys;
	private final CompletableFuture<Text> future = new CompletableFuture<>();
	private int sentTick = -1;

	public QueuedCommand(String command, Set<String> successKeys, Set<String> harmlessKeys) {
		this.command = command;
		this.successKeys = successKeys;
		this.harmlessKeys = harmlessKeys;
	}

	public String command() {
		return this.command;
	}

	public boolean succeeds(String key) {
		return this.successKeys.contains(key);
	}

	public boolean harmless(String key) {
		return this.harmlessKeys.contains(key);
	}

	public CompletableFuture<Text> future() {
		return this.future;
	}

	public int sentTick() {
		return this.sentTick;
	}

	public void sent(int tick) {
		this.sentTick = tick;
	}
}
