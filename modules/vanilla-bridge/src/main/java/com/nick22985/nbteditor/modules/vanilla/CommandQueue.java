package com.nick22985.nbteditor.modules.vanilla;

import com.nick22985.nbteditor.hook.ChatReceiveEvent;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TranslatableContent;
import com.nick22985.nbteditor.util.TextQueryUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandQueue {
	private static final int PER_TICK = 5;
	private static final int MAX_IN_FLIGHT = 40;
	private static final int TIMEOUT_TICKS = 100;
	private static final int SPAM_COST = 20;
	private static final int SPAM_LIMIT = 160;
	private static final List<String> ERROR_PREFIXES = List.of("command.", "commands.", "argument.", "arguments.", "parsing.", "permissions.");
	private static final String CONTEXT_KEY = "command.context.here";

	private final ModuleContext ctx;
	private final Deque<QueuedCommand> waiting = new ArrayDeque<>();
	private final Deque<QueuedCommand> inFlight = new ArrayDeque<>();
	private int tick;
	private int lastFailureTick = -100;
	private int spam;

	public CommandQueue(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public CompletableFuture<Text> submit(String command, Set<String> successKeys, Set<String> harmlessKeys) {
		QueuedCommand queued = new QueuedCommand(command, successKeys, harmlessKeys);
		this.waiting.add(queued);
		return queued.future();
	}

	public boolean busy() {
		return !this.waiting.isEmpty() || !this.inFlight.isEmpty();
	}

	public boolean spamExempt() {
		return this.ctx.client().player().permissionLevel() > 0;
	}

	public void tick() {
		this.tick++;
		if (this.spam > 0) {
			this.spam--;
		}
		QueuedCommand head = this.inFlight.peek();
		if (head != null && this.tick - head.sentTick() > TIMEOUT_TICKS) {
			this.failAll(new TargetException(this.ctx.tr("error.timeout")));
			return;
		}
		int sent = 0;
		boolean exempt = this.spamExempt();
		while (sent < PER_TICK && this.inFlight.size() < MAX_IN_FLIGHT && !this.waiting.isEmpty() && (exempt || this.spam + SPAM_COST <= SPAM_LIMIT)) {
			QueuedCommand next = this.waiting.poll();
			next.sent(this.tick);
			this.inFlight.add(next);
			this.ctx.client().commands().send(next.command());
			this.spam += SPAM_COST;
			sent++;
		}
	}

	public void failAll(RuntimeException error) {
		while (!this.inFlight.isEmpty()) {
			this.inFlight.poll().future().completeExceptionally(error);
		}
		while (!this.waiting.isEmpty()) {
			this.waiting.poll().future().completeExceptionally(error);
		}
	}

	public void receive(ChatReceiveEvent event) {
		if (event.overlay() || this.inFlight.isEmpty()) {
			return;
		}
		Text message = event.message();
		if (TextQueryUtil.containsKey(message, CONTEXT_KEY)) {
			if (this.tick - this.lastFailureTick <= 2) {
				event.cancel();
			}
			return;
		}
		TranslatableContent content = TextQueryUtil.firstTranslatable(message);
		if (content == null) {
			return;
		}
		QueuedCommand head = this.inFlight.peek();
		String key = content.key();
		boolean red = TextQueryUtil.isRed(message);
		if (head.harmless(key) || !red && (head.succeeds(key) || key.startsWith("commands."))) {
			this.inFlight.poll();
			event.cancel();
			head.future().complete(message);
			return;
		}
		if (red || this.errorKey(key)) {
			this.inFlight.poll();
			event.cancel();
			this.lastFailureTick = this.tick;
			this.ctx.log().debug("Command failed: " + head.command() + " -> " + message.plain());
			head.future().completeExceptionally(new TargetException(this.ctx.tr("error.command", message)));
		}
	}

	private boolean errorKey(String key) {
		if (key.startsWith("chat.")) {
			return false;
		}
		for (String prefix : ERROR_PREFIXES) {
			if (key.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
