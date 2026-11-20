package com.nick22985.nbteditor.net;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NetException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NetException(String message) {
		super(message);
	}

	public NetException(String message, Throwable cause) {
		super(message, cause);
	}
}
