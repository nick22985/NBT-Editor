package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.UuidUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class AttributeModifiersField implements Field<List<AttributeModifier>> {
	private static final String COMPONENT = "attribute_modifiers";
	private static final String LEGACY = "AttributeModifiers";
	private static final Set<String> COMPONENT_KEYS = Set.of("type", "amount", "operation", "slot", "id", "uuid");
	private static final Set<String> LEGACY_KEYS = Set.of("AttributeName", "Amount", "Operation", "Slot", "UUID");

	@Override
	public String id() {
		return COMPONENT;
	}

	@Override
	public FieldValue<List<AttributeModifier>> read(CompoundTag item, GameVersion version) {
		boolean components = Feature.ITEM_COMPONENTS.in(version);
		Tag tag = components ? ItemStackUtil.component(item, COMPONENT) : ItemDataUtil.dataTag(item, version, LEGACY);
		if (tag == null) {
			return FieldValue.absent();
		}
		if (tag instanceof CompoundTag && components) {
			tag = ((CompoundTag) tag).getList("modifiers");
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		List<AttributeModifier> modifiers = new ArrayList<>();
		for (Tag element : (ListTag) tag) {
			if (element instanceof CompoundTag) {
				AttributeModifier modifier = components ? this.readComponent((CompoundTag) element) : this.readLegacy((CompoundTag) element);
				if (modifier != null) {
					modifiers.add(modifier);
				}
			}
		}
		return FieldValue.of(modifiers);
	}

	private CompoundTag extra(CompoundTag tag, Set<String> known) {
		CompoundTag extra = new CompoundTag();
		for (String key : tag.keys()) {
			if (!known.contains(key)) {
				extra.put(key, tag.get(key).copy());
			}
		}
		return extra;
	}

	private AttributeModifier readComponent(CompoundTag tag) {
		AttributeOperation operation = AttributeOperation.of(tag.getString("operation", ""));
		NumericTag amount = tag.getNumber("amount");
		if (operation == null || amount == null || !tag.contains("type")) {
			return null;
		}
		String id = tag.getString("id", null);
		if (id == null) {
			UUID uuid = UuidUtil.fromTag(tag.get("uuid"));
			id = uuid == null ? null : uuid.toString();
		}
		return new AttributeModifier(ItemStackUtil.namespaced(tag.getString("type", "")), amount.doubleValue(), operation, tag.getString("slot", AttributeModifier.ANY), id, this.extra(tag, COMPONENT_KEYS));
	}

	private AttributeModifier readLegacy(CompoundTag tag) {
		AttributeOperation operation = AttributeOperation.of(tag.getInt("Operation", -1));
		NumericTag amount = tag.getNumber("Amount");
		if (operation == null || amount == null || !tag.contains("AttributeName")) {
			return null;
		}
		UUID uuid = UuidUtil.fromTag(tag.get("UUID"));
		return new AttributeModifier(ItemStackUtil.namespaced(tag.getString("AttributeName", "")), amount.doubleValue(), operation, tag.getString("Slot", AttributeModifier.ANY), uuid == null ? null : uuid.toString(), this.extra(tag, LEGACY_KEYS));
	}

	@Override
	public void write(CompoundTag item, GameVersion version, List<AttributeModifier> value) {
		boolean components = Feature.ITEM_COMPONENTS.in(version);
		if (value == null) {
			if (components) {
				ItemStackUtil.setComponent(item, COMPONENT, null);
			} else {
				ItemDataUtil.putData(item, version, LEGACY, null);
			}
			return;
		}
		ListTag list = new ListTag();
		for (AttributeModifier modifier : value) {
			list.add(components ? this.writeComponent(modifier, version) : this.writeLegacy(modifier));
		}
		if (!components) {
			ItemDataUtil.putData(item, version, LEGACY, list);
			return;
		}
		if (Feature.TOOLTIP_DISPLAY.in(version)) {
			ItemStackUtil.setComponent(item, COMPONENT, list);
			return;
		}
		Tag existing = ItemStackUtil.component(item, COMPONENT);
		CompoundTag compound = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
		compound.put("modifiers", list);
		ItemStackUtil.setComponent(item, COMPONENT, compound);
	}

	private String uuid(AttributeModifier modifier) {
		if (modifier.id() != null) {
			try {
				return UUID.fromString(modifier.id()).toString();
			} catch (IllegalArgumentException e) {
				return UUID.nameUUIDFromBytes(modifier.id().getBytes(StandardCharsets.UTF_8)).toString();
			}
		}
		return UUID.randomUUID().toString();
	}

	private CompoundTag writeComponent(AttributeModifier modifier, GameVersion version) {
		CompoundTag tag = modifier.extra() == null ? new CompoundTag() : modifier.extra().copy();
		tag.putString("type", modifier.attribute());
		tag.putDouble("amount", modifier.amount());
		tag.putString("operation", modifier.operation().serializedName());
		tag.putString("slot", modifier.slot());
		if (version.atLeast(GameVersion.V1_21)) {
			String id = modifier.id() == null ? "minecraft:" + UUID.randomUUID() : modifier.id();
			tag.putString("id", id.indexOf(':') >= 0 ? id : "minecraft:" + id);
			tag.remove("name");
		} else {
			tag.put("uuid", UuidUtil.toIntArray(UUID.fromString(this.uuid(modifier))));
			if (!tag.contains("name")) {
				tag.put("name", StringTag.of(modifier.attribute()));
			}
		}
		return tag;
	}

	private CompoundTag writeLegacy(AttributeModifier modifier) {
		CompoundTag tag = modifier.extra() == null ? new CompoundTag() : modifier.extra().copy();
		tag.putString("AttributeName", modifier.attribute());
		if (!tag.contains("Name")) {
			tag.putString("Name", modifier.attribute());
		}
		tag.putDouble("Amount", modifier.amount());
		tag.putInt("Operation", modifier.operation().ordinal());
		if (AttributeModifier.ANY.equals(modifier.slot())) {
			tag.remove("Slot");
		} else {
			tag.putString("Slot", modifier.slot());
		}
		tag.put("UUID", UuidUtil.toIntArray(UUID.fromString(this.uuid(modifier))));
		return tag;
	}
}
