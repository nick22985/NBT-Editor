package com.nick22985.nbteditor.fabric.util;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.nick22985.nbteditor.platform.DataKind;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McDataFixUtil {
	private McDataFixUtil() {
	}

	public static int dataVersion() {
		return SharedConstants.WORLD_VERSION;
	}

	public static DSL.TypeReference reference(DataKind kind) {
		switch (kind) {
			case ENTITY:
				return References.ENTITY;
			case BLOCK_ENTITY:
				return References.BLOCK_ENTITY;
			case BLOCK_STATE:
				return References.BLOCK_STATE;
			default:
				return References.ITEM_STACK;
		}
	}

	public static com.nick22985.nbteditor.nbt.CompoundTag upgrade(DataKind kind, com.nick22985.nbteditor.nbt.CompoundTag tag, int fromDataVersion) {
		int to = dataVersion();
		if (fromDataVersion <= 0 || fromDataVersion >= to) {
			return tag.copy();
		}
		DataFixer fixer = DataFixers.getDataFixer();
		Tag result = fixer.update(reference(kind), new Dynamic<>(NbtOps.INSTANCE, (Tag) McTagUtil.toMc(tag)), fromDataVersion, to).getValue();
		return result instanceof CompoundTag compound ? McTagUtil.fromMc(compound) : tag.copy();
	}
}
