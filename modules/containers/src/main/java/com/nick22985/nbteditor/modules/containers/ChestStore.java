package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ChestStore extends ItemsStore {
	private static final int COLUMNS = 5;
	private static final int ROWS = 3;
	private static final int LEGACY_OFFSET = 2;

	private final boolean llama;

	public ChestStore(boolean llama) {
		super(ITEMS, ContainerLayout.grid(COLUMNS, ROWS), LEGACY_OFFSET);
		this.llama = llama;
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		super.write(context, items);
		int last = -1;
		for (int i = 0; i < items.size(); i++) {
			if (!ItemStackUtil.isEmpty(items.get(i))) {
				last = i;
			}
		}
		if (last < 0) {
			return;
		}
		CompoundTag data = context.data();
		data.putBoolean("ChestedHorse", true);
		if (this.llama) {
			int strength = last / ROWS + 1;
			if (data.getInt("Strength", 0) < strength) {
				data.putInt("Strength", strength);
			}
		}
		context.commit(data);
	}
}
