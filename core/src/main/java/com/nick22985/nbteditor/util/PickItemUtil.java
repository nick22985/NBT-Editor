package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PickItemUtil {
	public static final String SPAWN_EGG = "_spawn_egg";
	public static final String ENTITY_COMPONENT = "entity_data";
	public static final String ENTITY_LEGACY = "EntityTag";
	public static final List<String> POSITION_KEYS = List.of("x", "y", "z", "keepPacked");
	public static final List<String> PLACEMENT_KEYS = List.of("Pos", "Motion", "Rotation", "TileX", "TileY", "TileZ", "block_pos", "Facing", "facing");
	private static final String BOAT = "minecraft:boat";
	private static final String CHEST_BOAT = "minecraft:chest_boat";
	private static final String BAMBOO = "bamboo";
	private static final List<String> ENTITY_KEYS = List.of("Pos", "Motion", "UUID", "Passengers", "Rotation", "TileX", "TileY", "TileZ", "block_pos", "Facing", "facing");

	private PickItemUtil() {
	}

	public static CompoundTag block(BlockData data, String itemId, Map<String, String> defaults, GameVersion version) {
		CompoundTag item = ItemStackUtil.create(itemId, 1, version);
		Map<String, String> changed = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : data.properties().entrySet()) {
			if (!entry.getValue().equals(defaults.get(entry.getKey()))) {
				changed.put(entry.getKey(), entry.getValue());
			}
		}
		if (!changed.isEmpty()) {
			BlockStateUtil.write(item, true, version, changed);
		}
		if (data.blockEntity() != null && !data.blockEntity().isEmpty()) {
			CompoundTag blockEntity = data.blockEntity().copy();
			for (String key : POSITION_KEYS) {
				blockEntity.remove(key);
			}
			BlockEntityUtil.write(item, true, version, blockEntity.getString("id", ""), blockEntity);
		}
		return item;
	}

	public static String entityItem(String entityId, CompoundTag entity, Predicate<String> itemExists) {
		String id = ItemStackUtil.namespaced(entityId);
		String boat = boatItem(id, entity);
		if (boat != null && itemExists.test(boat)) {
			return boat;
		}
		String egg = id + SPAWN_EGG;
		if (itemExists.test(egg)) {
			return egg;
		}
		return itemExists.test(id) ? id : null;
	}

	public static String boatItem(String entityId, CompoundTag entity) {
		boolean chest = CHEST_BOAT.equals(entityId);
		if (!chest && !BOAT.equals(entityId) || entity == null) {
			return null;
		}
		String wood = ItemStackUtil.stripNamespace(entity.getString("Type", "oak"));
		if (BAMBOO.equals(wood)) {
			return chest ? "minecraft:bamboo_chest_raft" : "minecraft:bamboo_raft";
		}
		return "minecraft:" + wood + (chest ? "_chest_boat" : "_boat");
	}

	public static CompoundTag entity(String entityId, CompoundTag entity, String itemId, GameVersion version) {
		CompoundTag item = ItemStackUtil.create(itemId, 1, version);
		CompoundTag data = entity.copy();
		for (String key : ENTITY_KEYS) {
			data.remove(key);
		}
		data.putString("id", ItemStackUtil.namespaced(entityId));
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, ENTITY_COMPONENT, data);
		} else {
			ItemDataUtil.putData(item, version, ENTITY_LEGACY, data);
		}
		return item;
	}

	public static CompoundTag summonable(CompoundTag entity) {
		CompoundTag copy = entity.copy();
		copy.remove("UUID");
		ListTag passengers = copy.getList("Passengers");
		if (passengers != null) {
			ListTag stripped = new ListTag();
			for (Tag passenger : passengers) {
				stripped.add(passenger instanceof CompoundTag ? summonable((CompoundTag) passenger) : passenger);
			}
			copy.put("Passengers", stripped);
		}
		return copy;
	}
}
