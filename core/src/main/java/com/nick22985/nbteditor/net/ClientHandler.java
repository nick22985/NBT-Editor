package com.nick22985.nbteditor.net;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ClientHandler<T> {
	void handle(T message);
}
