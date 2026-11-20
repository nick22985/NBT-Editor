package com.nick22985.nbteditor.net;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FrameAssembler {
	public static final int MAX_MESSAGE = 32 * 1024 * 1024;
	public static final int MAX_PENDING = 16;

	private final Map<Integer, PendingMessage> pending = new LinkedHashMap<>();

	public AssembledMessage accept(Frame frame) {
		if (frame.parts() == 1) {
			return new AssembledMessage(frame.type(), frame.payload());
		}
		PendingMessage message = this.pending.get(frame.messageId());
		if (frame.part() == 0) {
			if (this.pending.size() >= MAX_PENDING) {
				Integer oldest = this.pending.keySet().iterator().next();
				this.pending.remove(oldest);
			}
			message = new PendingMessage(frame.type(), frame.parts());
			this.pending.put(frame.messageId(), message);
		}
		if (message == null || message.parts() != frame.parts() || frame.part() != message.received()) {
			this.pending.remove(frame.messageId());
			throw new NetException("out of order frame " + frame.part() + "/" + frame.parts() + " for message " + frame.messageId());
		}
		message.append(frame.payload());
		if (message.size() > MAX_MESSAGE) {
			this.pending.remove(frame.messageId());
			throw new NetException("message too large");
		}
		if (message.received() == message.parts()) {
			this.pending.remove(frame.messageId());
			return new AssembledMessage(message.type(), message.bytes());
		}
		return null;
	}

	public void clear() {
		this.pending.clear();
	}
}
