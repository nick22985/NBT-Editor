package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.util.McDataFixUtil;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.DataFixService;
import com.nick22985.nbteditor.platform.DataKind;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricDataFixService implements DataFixService {
	@Override
	public int dataVersion() {
		return McDataFixUtil.dataVersion();
	}

	@Override
	public CompoundTag upgrade(DataKind kind, CompoundTag tag, int fromDataVersion) {
		return McDataFixUtil.upgrade(kind, tag, fromDataVersion);
	}
}
