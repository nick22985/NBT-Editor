package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockDocUtil {
	public static final String NAME = "Name";
	public static final String PROPERTIES = "Properties";
	public static final String NBT = "nbt";

	private BlockDocUtil() {
	}

	public static CompoundTag toTag(BlockData data) {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, data.blockId());
		CompoundTag properties = new CompoundTag();
		for (Map.Entry<String, String> entry : data.properties().entrySet()) {
			properties.putString(entry.getKey(), entry.getValue());
		}
		tag.put(PROPERTIES, properties);
		tag.put(NBT, data.blockEntity() == null ? new CompoundTag() : data.blockEntity().copy());
		return tag;
	}

	public static BlockData fromTag(CompoundTag tag) {
		Map<String, String> properties = new LinkedHashMap<>();
		CompoundTag stored = tag.getCompound(PROPERTIES);
		if (stored != null) {
			for (Map.Entry<String, Tag> entry : stored.entries()) {
				Tag value = entry.getValue();
				properties.put(entry.getKey(), value instanceof StringTag ? ((StringTag) value).value() : value instanceof NumericTag ? String.valueOf(((NumericTag) value).number()) : SnbtUtil.write(value, SnbtDialect.MODERN));
			}
		}
		CompoundTag nbt = tag.getCompound(NBT);
		return new BlockData(ItemStackUtil.namespaced(tag.getString(NAME, "minecraft:air")), properties, nbt == null ? null : nbt.copy());
	}

	public static BlockData parseSpec(String input, SnbtDialect dialect) throws SnbtException {
		String spec = input.trim();
		int end = 0;
		while (end < spec.length() && spec.charAt(end) != '[' && spec.charAt(end) != '{') {
			end++;
		}
		String id = spec.substring(0, end).trim();
		if (id.isEmpty()) {
			throw new SnbtException("Expected a block id", input, 0);
		}
		Map<String, String> properties = new LinkedHashMap<>();
		if (end < spec.length() && spec.charAt(end) == '[') {
			int close = spec.indexOf(']', end);
			if (close < 0) {
				throw new SnbtException("Expected ]", input, spec.length());
			}
			for (String pair : spec.substring(end + 1, close).split(",")) {
				if (pair.isBlank()) {
					continue;
				}
				int equals = pair.indexOf('=');
				if (equals < 0) {
					throw new SnbtException("Expected key=value", input, end + 1);
				}
				properties.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
			}
			end = close + 1;
		}
		String rest = spec.substring(end).trim();
		CompoundTag nbt = rest.isEmpty() ? null : SnbtUtil.parseCompound(rest, dialect);
		return new BlockData(ItemStackUtil.namespaced(id), properties, nbt);
	}
}
