package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.util.BlockStateUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookshelfStore extends ItemsStore {
	public static final int SLOTS = 6;

	public BookshelfStore() {
		super(ITEMS, ContainerLayout.grid(3, 2), 0);
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		super.write(context, items);
		CompoundTag root = context.subject().root();
		Map<String, String> properties = BlockStateUtil.read(root, context.item(), context.version());
		for (int i = 0; i < SLOTS; i++) {
			String key = "slot_" + i + "_occupied";
			boolean occupied = i < items.size() && !ItemStackUtil.isEmpty(items.get(i));
			if (!context.item()) {
				properties.put(key, String.valueOf(occupied));
			} else if (occupied) {
				properties.put(key, "true");
			} else {
				properties.remove(key);
			}
		}
		BlockStateUtil.write(root, context.item(), context.version(), properties);
	}
}
