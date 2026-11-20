package com.nick22985.nbteditor.platform;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface PickListener {
	boolean enabled();

	boolean debug();

	GameItem finish(GameItem item);

	void picked(BlockData block, GameItem item, boolean includeData);
}
