package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class UuidUtil {
	private UuidUtil() {
	}

	public static IntArrayTag toIntArray(UUID uuid) {
		long most = uuid.getMostSignificantBits();
		long least = uuid.getLeastSignificantBits();
		return new IntArrayTag(new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
	}

	public static UUID fromIntArray(int[] value) {
		if (value == null || value.length != 4) {
			return null;
		}
		return new UUID((long) value[0] << 32 | value[1] & 0xFFFFFFFFL, (long) value[2] << 32 | value[3] & 0xFFFFFFFFL);
	}

	public static UUID parse(String value) {
		if (value == null) {
			return null;
		}
		String text = value.length() == 32 ? value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20) : value;
		try {
			return UUID.fromString(text);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static UUID fromTag(Tag tag) {
		if (tag instanceof IntArrayTag) {
			return fromIntArray(((IntArrayTag) tag).value());
		}
		if (tag instanceof StringTag) {
			return parse(((StringTag) tag).value());
		}
		return null;
	}
}
