package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PresetItem(Text name, Function<GameVersion, CompoundTag> factory) {
	public static final RegistryKey<PresetItem> REGISTRY = RegistryKey.of("preset_item", PresetItem.class);

	public CompoundTag create(GameVersion version) {
		return this.factory.apply(version);
	}
}
