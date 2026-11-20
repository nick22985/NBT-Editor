package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface SlotStore {
	ContainerLayout layout(StoreContext context);

	List<CompoundTag> read(StoreContext context);

	void write(StoreContext context, List<CompoundTag> items);
}
