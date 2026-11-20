package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockStateUtil {
	public static final String LECTERN = "minecraft:lectern";
	public static final String JUKEBOX = "minecraft:jukebox";
	public static final String BREWING_STAND = "minecraft:brewing_stand";
	public static final String ITEM_COMPONENT = "block_state";
	public static final String ITEM_LEGACY = "BlockStateTag";
	private static final int BOTTLES = 3;

	private BlockStateUtil() {
	}

	public static Map<String, String> read(CompoundTag root, boolean item, GameVersion version) {
		Tag tag;
		if (!item) {
			tag = root.get(BlockDocUtil.PROPERTIES);
		} else if (Feature.ITEM_COMPONENTS.in(version)) {
			tag = ItemStackUtil.component(root, ITEM_COMPONENT);
		} else {
			CompoundTag data = ItemStackUtil.data(root, version);
			tag = data == null ? null : data.get(ITEM_LEGACY);
		}
		Map<String, String> properties = new LinkedHashMap<>();
		if (tag instanceof CompoundTag) {
			for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
				Tag value = entry.getValue();
				if (value instanceof StringTag) {
					properties.put(entry.getKey(), ((StringTag) value).value());
				} else if (value instanceof NumericTag) {
					properties.put(entry.getKey(), String.valueOf(((NumericTag) value).intValue()));
				}
			}
		}
		return properties;
	}

	public static void write(CompoundTag root, boolean item, GameVersion version, Map<String, String> properties) {
		CompoundTag tag = new CompoundTag();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			tag.putString(entry.getKey(), entry.getValue());
		}
		if (!item) {
			root.put(BlockDocUtil.PROPERTIES, tag);
		} else if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(root, ITEM_COMPONENT, tag.isEmpty() ? null : tag);
		} else if (tag.isEmpty()) {
			CompoundTag data = ItemStackUtil.data(root, version);
			if (data != null) {
				data.remove(ITEM_LEGACY);
				ItemStackUtil.pruneData(root, version);
			}
		} else {
			ItemStackUtil.dataOrCreate(root, version).put(ITEM_LEGACY, tag);
		}
	}

	public static BlockData sync(BlockData data) {
		CompoundTag blockEntity = data.blockEntity();
		if (blockEntity == null) {
			return data;
		}
		Map<String, String> properties = new LinkedHashMap<>(data.properties());
		switch (data.blockId()) {
			case LECTERN:
				set(properties, "has_book", present(blockEntity.get("Book")));
				break;
			case JUKEBOX:
				set(properties, "has_record", present(blockEntity.get("RecordItem")));
				break;
			case BREWING_STAND:
				boolean[] bottles = new boolean[BOTTLES];
				ListTag items = blockEntity.getList("Items");
				if (items != null) {
					for (Tag item : items) {
						if (item instanceof CompoundTag && present(item)) {
							int slot = ((CompoundTag) item).getInt("Slot", -1);
							if (slot >= 0 && slot < BOTTLES) {
								bottles[slot] = true;
							}
						}
					}
				}
				for (int i = 0; i < BOTTLES; i++) {
					set(properties, "has_bottle_" + i, bottles[i]);
				}
				break;
			default:
				return data;
		}
		return new BlockData(data.blockId(), properties, blockEntity);
	}

	private static boolean present(Tag item) {
		return item instanceof CompoundTag && !ItemStackUtil.isEmpty((CompoundTag) item);
	}

	private static void set(Map<String, String> properties, String key, boolean value) {
		if (properties.containsKey(key)) {
			properties.put(key, String.valueOf(value));
		}
	}
}
