package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface DataFixService {
	int dataVersion();

	CompoundTag upgrade(DataKind kind, CompoundTag tag, int fromDataVersion);
}
