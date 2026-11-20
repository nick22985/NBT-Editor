package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ContainerHost {
	CompoundTag read(ContainerSubject subject);

	void write(ContainerSubject subject, CompoundTag data);

	default boolean containerComponent(ContainerSubject subject) {
		return false;
	}
}
