package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EntityAttributesField implements Field<Map<String, Double>> {
	private String listKey(GameVersion version) {
		return version.atLeast(GameVersion.V1_21) ? "attributes" : "Attributes";
	}

	private String idKey(GameVersion version) {
		return version.atLeast(GameVersion.V1_21) ? "id" : "Name";
	}

	private String baseKey(GameVersion version) {
		return version.atLeast(GameVersion.V1_21) ? "base" : "Base";
	}

	@Override
	public String id() {
		return "attributes";
	}

	@Override
	public FieldValue<Map<String, Double>> read(CompoundTag entity, GameVersion version) {
		Tag tag = entity.get(this.listKey(version));
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		Map<String, Double> values = new LinkedHashMap<>();
		for (Tag element : (ListTag) tag) {
			if (!(element instanceof CompoundTag)) {
				continue;
			}
			CompoundTag attribute = (CompoundTag) element;
			String id = attribute.getString(this.idKey(version));
			NumericTag base = attribute.getNumber(this.baseKey(version));
			if (id != null && base != null) {
				values.put(ItemStackUtil.namespaced(id), base.doubleValue());
			}
		}
		return FieldValue.of(values);
	}

	@Override
	public void write(CompoundTag entity, GameVersion version, Map<String, Double> value) {
		String listKey = this.listKey(version);
		if (value == null) {
			entity.remove(listKey);
			return;
		}
		Map<String, CompoundTag> existing = new LinkedHashMap<>();
		Tag tag = entity.get(listKey);
		if (tag instanceof ListTag) {
			for (Tag element : (ListTag) tag) {
				if (element instanceof CompoundTag) {
					String id = ((CompoundTag) element).getString(this.idKey(version));
					if (id != null) {
						existing.put(ItemStackUtil.namespaced(id), (CompoundTag) element);
					}
				}
			}
		}
		ListTag list = new ListTag();
		for (Map.Entry<String, Double> entry : value.entrySet()) {
			CompoundTag previous = existing.get(entry.getKey());
			CompoundTag attribute = previous == null ? new CompoundTag() : previous.copy();
			attribute.putString(this.idKey(version), entry.getKey());
			attribute.putDouble(this.baseKey(version), entry.getValue());
			list.add(attribute);
		}
		entity.put(listKey, list);
	}
}
