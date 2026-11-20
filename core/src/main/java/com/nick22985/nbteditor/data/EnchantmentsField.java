package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.ShortTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EnchantmentsField implements Field<Map<String, Integer>> {
	private final String component;
	private final String legacyKey;

	public EnchantmentsField(String component, String legacyKey) {
		this.component = component;
		this.legacyKey = legacyKey;
	}

	@Override
	public String id() {
		return component;
	}

	@Override
	public FieldValue<Map<String, Integer>> read(CompoundTag item, GameVersion version) {
		Map<String, Integer> levels = new LinkedHashMap<>();
		if (Feature.ITEM_COMPONENTS.in(version)) {
			Tag tag = ItemStackUtil.component(item, component);
			if (tag == null) {
				return FieldValue.absent();
			}
			if (!(tag instanceof CompoundTag)) {
				return FieldValue.invalid("not_compound");
			}
			CompoundTag compound = (CompoundTag) tag;
			CompoundTag source = compound.getCompound("levels") != null ? compound.getCompound("levels") : compound;
			for (Map.Entry<String, Tag> entry : source.entries()) {
				if (entry.getValue() instanceof NumericTag) {
					levels.put(ItemStackUtil.namespaced(entry.getKey()), ((NumericTag) entry.getValue()).intValue());
				}
			}
			return FieldValue.of(levels);
		}
		Tag tag = ItemDataUtil.dataTag(item, version, legacyKey);
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		for (Tag element : (ListTag) tag) {
			if (element instanceof CompoundTag) {
				CompoundTag entry = (CompoundTag) element;
				levels.put(ItemStackUtil.namespaced(entry.getString("id", "")), entry.getInt("lvl", 1));
			}
		}
		return FieldValue.of(levels);
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Map<String, Integer> value) {
		if (Feature.ITEM_COMPONENTS.in(version)) {
			if (value == null || value.isEmpty()) {
				ItemStackUtil.setComponent(item, component, null);
				return;
			}
			CompoundTag levels = new CompoundTag();
			for (Map.Entry<String, Integer> entry : value.entrySet()) {
				levels.putInt(ItemStackUtil.namespaced(entry.getKey()), Math.max(1, entry.getValue()));
			}
			if (Feature.TOOLTIP_DISPLAY.in(version)) {
				ItemStackUtil.setComponent(item, component, levels);
			} else {
				Tag existing = ItemStackUtil.component(item, component);
				CompoundTag compound = existing instanceof CompoundTag && ((CompoundTag) existing).contains("levels") ? (CompoundTag) existing.copy() : new CompoundTag();
				compound.put("levels", levels);
				ItemStackUtil.setComponent(item, component, compound);
			}
			return;
		}
		if (value == null || value.isEmpty()) {
			ItemDataUtil.putData(item, version, legacyKey, null);
			return;
		}
		ListTag list = new ListTag();
		for (Map.Entry<String, Integer> entry : value.entrySet()) {
			CompoundTag enchantment = new CompoundTag();
			enchantment.putString("id", ItemStackUtil.namespaced(entry.getKey()));
			enchantment.put("lvl", ShortTag.of((short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, entry.getValue()))));
			list.add(enchantment);
		}
		ItemDataUtil.putData(item, version, legacyKey, list);
	}
}
