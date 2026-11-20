package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.DropChanceSlot;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.MobFlag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.FloatTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MobUtil {
	public static final float DEFAULT_DROP_CHANCE = 0.085F;
	public static final float PRESERVED_DROP_CHANCE = 2.0F;
	public static final float MAX_DROP_CHANCE = 100.0F;
	public static final float MAX_HEALTH = 1024.0F;
	public static final String ITEM_COMPONENT = "entity_data";
	public static final String ITEM_LEGACY = "EntityTag";
	public static final String DROP_CHANCES = "drop_chances";
	public static final String BODY_ARMOR_DROP_CHANCE = "body_armor_drop_chance";
	public static final String HEALTH = "Health";
	public static final String SPAWN_EGG = "_spawn_egg";
	private static final List<String> MOB_KEYS = List.of(MobFlag.PERSISTENCE_REQUIRED.key(), MobFlag.CAN_PICK_UP_LOOT.key(), MobFlag.LEFT_HANDED.key(), DropChanceSlot.ARMOR, DropChanceSlot.HAND, DROP_CHANCES);

	private MobUtil() {
	}

	public static boolean isSpawnEgg(String itemId) {
		return itemId != null && itemId.endsWith(SPAWN_EGG);
	}

	public static String eggEntity(String itemId) {
		if (!isSpawnEgg(itemId)) {
			return null;
		}
		String id = ItemStackUtil.namespaced(itemId);
		return id.substring(0, id.length() - SPAWN_EGG.length());
	}

	public static String entityId(CompoundTag root, boolean item, GameVersion version) {
		String id = read(root, item, version).getString("id");
		if (id != null && !id.isEmpty()) {
			return ItemStackUtil.namespaced(id);
		}
		return item ? eggEntity(ItemStackUtil.id(root)) : null;
	}

	public static boolean looksLikeMob(CompoundTag data) {
		for (String key : MOB_KEYS) {
			if (data.contains(key)) {
				return true;
			}
		}
		return false;
	}

	public static CompoundTag read(CompoundTag root, boolean item, GameVersion version) {
		if (!item) {
			return root.copy();
		}
		Tag tag = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(root, ITEM_COMPONENT) : ItemDataUtil.dataTag(root, version, ITEM_LEGACY);
		return tag instanceof CompoundTag ? ((CompoundTag) tag).copy() : new CompoundTag();
	}

	public static void write(CompoundTag root, boolean item, GameVersion version, CompoundTag data) {
		if (!item) {
			ContainerUtil.replace(root, data);
			return;
		}
		String egg = eggEntity(ItemStackUtil.id(root));
		String id = data.getString("id");
		boolean empty = data.isEmpty() || data.size() == 1 && id != null && egg != null && egg.equals(ItemStackUtil.namespaced(id));
		if (Feature.ITEM_COMPONENTS.in(version)) {
			if (!empty && id == null && egg != null) {
				data.putAt(0, "id", StringTag.of(egg));
			}
			ItemStackUtil.setComponent(root, ITEM_COMPONENT, empty ? null : data);
		} else {
			ItemDataUtil.putData(root, version, ITEM_LEGACY, empty ? null : data);
		}
	}

	public static List<DropChanceSlot> slots(GameVersion version) {
		List<DropChanceSlot> slots = new ArrayList<>();
		for (DropChanceSlot slot : DropChanceSlot.values()) {
			if (slot.in(version)) {
				slots.add(slot);
			}
		}
		return slots;
	}

	public static float dropChance(CompoundTag mob, DropChanceSlot slot, GameVersion version) {
		if (Feature.MOB_DROP_CHANCES.in(version)) {
			CompoundTag chances = mob.getCompound(DROP_CHANCES);
			return chances == null ? DEFAULT_DROP_CHANCE : chances.getFloat(slot.id(), DEFAULT_DROP_CHANCE);
		}
		if (slot.legacyList() == null) {
			return mob.getFloat(BODY_ARMOR_DROP_CHANCE, DEFAULT_DROP_CHANCE);
		}
		return legacyChance(mob.getList(slot.legacyList()), slot.legacyIndex());
	}

	public static void setDropChance(CompoundTag mob, DropChanceSlot slot, GameVersion version, float chance, boolean sparse) {
		boolean preset = chance == DEFAULT_DROP_CHANCE;
		if (Feature.MOB_DROP_CHANCES.in(version)) {
			if (!preset) {
				mob.getOrCreateCompound(DROP_CHANCES).putFloat(slot.id(), chance);
				return;
			}
			CompoundTag chances = mob.getCompound(DROP_CHANCES);
			if (chances != null) {
				chances.remove(slot.id());
				if (chances.isEmpty()) {
					mob.remove(DROP_CHANCES);
				}
			}
			return;
		}
		if (slot.legacyList() == null) {
			if (preset && sparse) {
				mob.remove(BODY_ARMOR_DROP_CHANCE);
			} else {
				mob.putFloat(BODY_ARMOR_DROP_CHANCE, chance);
			}
			return;
		}
		ListTag current = mob.getList(slot.legacyList());
		ListTag list = new ListTag();
		boolean allDefault = true;
		for (int i = 0; i < slot.legacySize(); i++) {
			float value = i == slot.legacyIndex() ? chance : legacyChance(current, i);
			allDefault &= value == DEFAULT_DROP_CHANCE;
			list.add(FloatTag.of(value));
		}
		if (allDefault && sparse) {
			mob.remove(slot.legacyList());
		} else {
			mob.put(slot.legacyList(), list);
		}
	}

	private static float legacyChance(ListTag list, int index) {
		if (list == null || index >= list.size() || !(list.get(index) instanceof NumericTag)) {
			return DEFAULT_DROP_CHANCE;
		}
		return ((NumericTag) list.get(index)).floatValue();
	}

	public static boolean flag(CompoundTag mob, MobFlag flag) {
		return mob.getBoolean(flag.key(), false);
	}

	public static void setFlag(CompoundTag mob, MobFlag flag, boolean value, boolean sparse) {
		if (!value && sparse) {
			mob.remove(flag.key());
		} else {
			mob.putBoolean(flag.key(), value);
		}
	}

	public static Float health(CompoundTag mob) {
		NumericTag health = mob.getNumber(HEALTH);
		return health == null ? null : health.floatValue();
	}

	public static void setHealth(CompoundTag mob, Float health) {
		if (health == null) {
			mob.remove(HEALTH);
		} else {
			mob.putFloat(HEALTH, health);
		}
	}
}
