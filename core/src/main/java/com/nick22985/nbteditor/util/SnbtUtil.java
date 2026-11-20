package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.SnbtReader;
import com.nick22985.nbteditor.nbt.SnbtWriter;
import com.nick22985.nbteditor.nbt.Tag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SnbtUtil {
	private SnbtUtil() {
	}

	public static Tag parse(String input, SnbtDialect dialect) throws SnbtException {
		return new SnbtReader(input, dialect).read();
	}

	public static Tag parse(String input, SnbtDialect dialect, boolean specialNumbers) throws SnbtException {
		return new SnbtReader(input, dialect, specialNumbers).read();
	}

	public static CompoundTag parseCompound(String input, SnbtDialect dialect) throws SnbtException {
		Tag tag = parse(input, dialect);
		if (!(tag instanceof CompoundTag)) {
			throw new SnbtException("Expected a compound", input, 0);
		}
		return (CompoundTag) tag;
	}

	public static String write(Tag tag, SnbtDialect dialect) {
		return SnbtWriter.compact(dialect).write(tag);
	}

	public static String pretty(Tag tag, SnbtDialect dialect) {
		return SnbtWriter.pretty(dialect).write(tag);
	}

	public static boolean isUnquotedChar(char c) {
		return c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '-' || c == '.' || c == '+';
	}
}
