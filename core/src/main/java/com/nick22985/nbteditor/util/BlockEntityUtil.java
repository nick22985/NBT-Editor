package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.Tag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockEntityUtil {
	public static final String ITEM_COMPONENT = "block_entity_data";
	public static final String ITEM_LEGACY = "BlockEntityTag";

	private BlockEntityUtil() {
	}

	public static String blockId(CompoundTag root, boolean item) {
		return item ? ItemStackUtil.id(root) : ItemStackUtil.namespaced(root.getString(BlockDocUtil.NAME, ItemStackUtil.AIR));
	}

	public static CompoundTag read(CompoundTag root, boolean item, GameVersion version) {
		Tag tag;
		if (!item) {
			tag = root.get(BlockDocUtil.NBT);
		} else if (Feature.ITEM_COMPONENTS.in(version)) {
			tag = ItemStackUtil.component(root, ITEM_COMPONENT);
		} else {
			tag = ItemDataUtil.dataTag(root, version, ITEM_LEGACY);
		}
		return tag instanceof CompoundTag ? ((CompoundTag) tag).copy() : new CompoundTag();
	}

	public static void write(CompoundTag root, boolean item, GameVersion version, String blockEntityId, CompoundTag data) {
		if (!item) {
			root.put(BlockDocUtil.NBT, data);
			return;
		}
		boolean empty = data.isEmpty() || data.size() == 1 && data.contains("id");
		if (Feature.ITEM_COMPONENTS.in(version)) {
			if (empty) {
				ItemStackUtil.setComponent(root, ITEM_COMPONENT, null);
				return;
			}
			if (!data.contains("id")) {
				data.putString("id", blockEntityId);
			}
			ItemStackUtil.setComponent(root, ITEM_COMPONENT, data);
		} else {
			ItemDataUtil.putData(root, version, ITEM_LEGACY, empty ? null : data);
		}
	}
}
