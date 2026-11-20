package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ItemService {
	GameItem fromTag(CompoundTag tag);

	GameItem empty();

	GameItem of(String id, int count);

	boolean isItem(String id);

	int maxStackSize(GameItem item);

	int dataVersion();

	CompoundTag upgrade(CompoundTag tag, int fromDataVersion);
}
