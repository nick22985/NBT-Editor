package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.PotionEffectUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotionContentsField implements Field<PotionContents> {
	private static final String COMPONENT = "potion_contents";

	@Override
	public String id() {
		return COMPONENT;
	}

	private String effectsKey(GameVersion version) {
		return Feature.STRING_EFFECT_IDS.in(version) ? "custom_potion_effects" : "CustomPotionEffects";
	}

	@Override
	public FieldValue<PotionContents> read(CompoundTag item, GameVersion version) {
		if (Feature.ITEM_COMPONENTS.in(version)) {
			return this.readComponent(item, version);
		}
		CompoundTag data = ItemStackUtil.data(item, version);
		if (data == null) {
			return FieldValue.absent();
		}
		Tag potion = data.get("Potion");
		Tag color = data.get("CustomPotionColor");
		ListTag effects = data.getList(this.effectsKey(version));
		if (potion == null && color == null && effects == null) {
			return FieldValue.absent();
		}
		return FieldValue.of(new PotionContents(potion instanceof StringTag ? ItemStackUtil.namespaced(((StringTag) potion).value()) : null, color instanceof NumericTag ? ((NumericTag) color).intValue() & 0xFFFFFF : null, null, this.effects(effects, version)));
	}

	private FieldValue<PotionContents> readComponent(CompoundTag item, GameVersion version) {
		Tag tag = ItemStackUtil.component(item, COMPONENT);
		if (tag == null || ItemStackUtil.isComponentRemoved(item, COMPONENT)) {
			return FieldValue.absent();
		}
		if (tag instanceof StringTag) {
			return FieldValue.of(PotionContents.EMPTY.withPotion(ItemStackUtil.namespaced(((StringTag) tag).value())));
		}
		if (!(tag instanceof CompoundTag)) {
			return FieldValue.invalid("not_compound");
		}
		CompoundTag contents = (CompoundTag) tag;
		Tag potion = contents.get("potion");
		Tag color = contents.get("custom_color");
		Tag name = contents.get("custom_name");
		return FieldValue.of(new PotionContents(potion instanceof StringTag ? ItemStackUtil.namespaced(((StringTag) potion).value()) : null, color instanceof NumericTag ? ((NumericTag) color).intValue() & 0xFFFFFF : null, name instanceof StringTag ? ((StringTag) name).value() : null, this.effects(contents.getList("custom_effects"), version)));
	}

	private List<PotionEffect> effects(ListTag list, GameVersion version) {
		List<PotionEffect> out = new ArrayList<>();
		if (list == null) {
			return out;
		}
		for (Tag entry : list) {
			if (entry instanceof CompoundTag) {
				PotionEffect effect = PotionEffectUtil.read((CompoundTag) entry, version);
				if (effect != null) {
					out.add(effect);
				}
			}
		}
		return out;
	}

	private ListTag effectList(List<PotionEffect> effects, GameVersion version) {
		ListTag list = new ListTag();
		for (PotionEffect effect : effects) {
			list.add(PotionEffectUtil.write(effect, version));
		}
		return list;
	}

	@Override
	public void write(CompoundTag item, GameVersion version, PotionContents value) {
		if (Feature.ITEM_COMPONENTS.in(version)) {
			this.writeComponent(item, version, value);
			return;
		}
		boolean empty = value == null;
		ItemDataUtil.putData(item, version, "Potion", empty || value.potion() == null ? null : StringTag.of(value.potion()));
		ItemDataUtil.putData(item, version, "CustomPotionColor", empty || value.customColor() == null ? null : IntTag.of(value.customColor()));
		ItemDataUtil.putData(item, version, this.effectsKey(version), empty || value.effects().isEmpty() ? null : this.effectList(value.effects(), version));
	}

	private void writeComponent(CompoundTag item, GameVersion version, PotionContents value) {
		if (value == null || value.isEmpty()) {
			ItemStackUtil.setComponent(item, COMPONENT, null);
			return;
		}
		Tag existing = ItemStackUtil.component(item, COMPONENT);
		CompoundTag contents = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
		this.put(contents, "potion", value.potion() == null ? null : StringTag.of(value.potion()));
		this.put(contents, "custom_color", value.customColor() == null ? null : IntTag.of(value.customColor()));
		if (Feature.POTION_CUSTOM_NAME.in(version)) {
			this.put(contents, "custom_name", value.customName() == null ? null : StringTag.of(value.customName()));
		}
		this.put(contents, "custom_effects", value.effects().isEmpty() ? null : this.effectList(value.effects(), version));
		ItemStackUtil.setComponent(item, COMPONENT, contents);
	}

	private void put(CompoundTag tag, String key, Tag value) {
		if (value == null) {
			tag.remove(key);
		} else {
			tag.put(key, value);
		}
	}
}
