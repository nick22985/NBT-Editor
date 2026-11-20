package com.nick22985.nbteditor.net;

import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ServerHandler<T> {
	void handle(UUID sender, T message);
}
