package com.nick22985.nbteditor.net;

import java.io.ByteArrayOutputStream;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PendingMessage {
	private final String type;
	private final int parts;
	private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	private int received;

	public PendingMessage(String type, int parts) {
		this.type = type;
		this.parts = parts;
	}

	public String type() {
		return this.type;
	}

	public int parts() {
		return this.parts;
	}

	public int received() {
		return this.received;
	}

	public int size() {
		return this.bytes.size();
	}

	public void append(byte[] payload) {
		this.bytes.write(payload, 0, payload.length);
		this.received++;
	}

	public byte[] bytes() {
		return this.bytes.toByteArray();
	}
}
