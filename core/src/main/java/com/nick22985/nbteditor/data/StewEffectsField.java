package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.PotionEffectUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StewEffectsField implements Field<List<PotionEffect>> {
	private static final String COMPONENT = "suspicious_stew_effects";

	@Override
	public String id() {
		return COMPONENT;
	}

	private String legacyKey(GameVersion version) {
		return Feature.STRING_EFFECT_IDS.in(version) ? "effects" : "Effects";
	}

	@Override
	public FieldValue<List<PotionEffect>> read(CompoundTag item, GameVersion version) {
		Tag tag = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(item, COMPONENT) : ItemDataUtil.dataTag(item, version, this.legacyKey(version));
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		List<PotionEffect> out = new ArrayList<>();
		for (Tag entry : (ListTag) tag) {
			if (entry instanceof CompoundTag) {
				PotionEffect effect = PotionEffectUtil.readStew((CompoundTag) entry, version);
				if (effect != null) {
					out.add(effect);
				}
			}
		}
		return FieldValue.of(out);
	}

	@Override
	public void write(CompoundTag item, GameVersion version, List<PotionEffect> value) {
		ListTag list = null;
		if (value != null && !value.isEmpty()) {
			list = new ListTag();
			for (PotionEffect effect : value) {
				list.add(PotionEffectUtil.writeStew(effect, version));
			}
		}
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, COMPONENT, list);
		} else {
			ItemDataUtil.putData(item, version, this.legacyKey(version), list);
		}
	}
}
