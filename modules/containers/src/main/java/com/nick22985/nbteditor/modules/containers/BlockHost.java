package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.ContainerUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockHost implements ContainerHost {
	private final String blockEntityId;

	public BlockHost(String blockEntityId) {
		this.blockEntityId = blockEntityId;
	}

	@Override
	public CompoundTag read(ContainerSubject subject) {
		return BlockEntityUtil.read(subject.root(), subject.item(), subject.version());
	}

	@Override
	public void write(ContainerSubject subject, CompoundTag data) {
		BlockEntityUtil.write(subject.root(), subject.item(), subject.version(), this.blockEntityId, subject.item() ? ContainerUtil.pruneEmpty(data) : data);
	}

	@Override
	public boolean containerComponent(ContainerSubject subject) {
		return subject.item() && Feature.ITEM_COMPONENTS.in(subject.version());
	}
}
