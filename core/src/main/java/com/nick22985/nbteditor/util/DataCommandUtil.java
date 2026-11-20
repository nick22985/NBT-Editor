package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtWriter;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Formatting;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DataCommandUtil {
	public static final int MAX_LENGTH = 255;

	private DataCommandUtil() {
	}

	public static List<String> diff(String prefix, String target, CompoundTag before, CompoundTag after, SnbtDialect dialect) {
		List<String> out = new ArrayList<>();
		diffCompound(prefix, target, NbtPath.ROOT, before == null ? new CompoundTag() : before, after, dialect, out);
		return out;
	}

	public static String snbt(Tag tag, SnbtDialect dialect) {
		String text = SnbtWriter.forCommands(dialect).write(tag);
		String safe = chatSafe(text, dialect);
		if (safe == null) {
			throw new IllegalArgumentException("illegal_characters");
		}
		return safe;
	}

	public static String chatSafe(String text, SnbtDialect dialect) {
		StringBuilder out = null;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (allowed(c)) {
				if (out != null) {
					out.append(c);
				}
				continue;
			}
			if (dialect != SnbtDialect.MODERN) {
				return null;
			}
			if (out == null) {
				out = new StringBuilder(text.length() + 8).append(text, 0, i);
			}
			out.append(String.format("\\u%04x", (int) c));
		}
		return out == null ? text : out.toString();
	}

	public static boolean allowed(char c) {
		return c != Formatting.SECTION && c >= ' ' && c != 127;
	}

	public static boolean allowedText(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (!allowed(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean fits(String command) {
		return command.length() <= MAX_LENGTH;
	}

	private static void diffCompound(String prefix, String target, NbtPath path, CompoundTag before, CompoundTag after, SnbtDialect dialect, List<String> out) {
		for (String key : before.keys()) {
			if (!after.contains(key)) {
				out.add(require(prefix + "data remove " + target + " " + path.child(key)));
			}
		}
		for (String key : after.keys()) {
			Tag next = after.get(key);
			Tag previous = before.get(key);
			if (next.equals(previous)) {
				continue;
			}
			update(prefix, target, path.child(key), previous, next, dialect, out);
		}
	}

	private static void update(String prefix, String target, NbtPath path, Tag before, Tag after, SnbtDialect dialect, List<String> out) {
		String set = prefix + "data modify " + target + " " + path + " set value " + snbt(after, dialect);
		if (fits(set)) {
			out.add(set);
			return;
		}
		if (after instanceof CompoundTag) {
			if (!(before instanceof CompoundTag)) {
				out.add(require(prefix + "data modify " + target + " " + path + " set value {}"));
			}
			diffCompound(prefix, target, path, before instanceof CompoundTag ? (CompoundTag) before : new CompoundTag(), (CompoundTag) after, dialect, out);
			return;
		}
		if (!after.type().isContainer()) {
			throw new IllegalArgumentException("too_long");
		}
		int size = TagArrayUtil.containerSize(after);
		if (before != null && before.type() == after.type() && TagArrayUtil.containerSize(before) == size) {
			for (int i = 0; i < size; i++) {
				Tag next = NbtPathUtil.step(after, i);
				Tag previous = NbtPathUtil.step(before, i);
				if (!next.equals(previous)) {
					update(prefix, target, path.child(i), previous, next, dialect, out);
				}
			}
			return;
		}
		out.add(require(prefix + "data modify " + target + " " + path + " set value " + snbt(empty(after), dialect)));
		for (int i = 0; i < size; i++) {
			Tag element = NbtPathUtil.step(after, i);
			String append = prefix + "data modify " + target + " " + path + " append value " + snbt(element, dialect);
			if (fits(append)) {
				out.add(append);
			} else if (element.type().isContainer()) {
				Tag start = empty(element);
				out.add(require(prefix + "data modify " + target + " " + path + " append value " + snbt(start, dialect)));
				update(prefix, target, path.child(i), start, element, dialect, out);
			} else {
				throw new IllegalArgumentException("too_long");
			}
		}
	}

	private static Tag empty(Tag tag) {
		switch (tag.type()) {
			case COMPOUND:
				return new CompoundTag();
			case BYTE_ARRAY:
				return new ByteArrayTag(new byte[0]);
			case INT_ARRAY:
				return new IntArrayTag(new int[0]);
			case LONG_ARRAY:
				return new LongArrayTag(new long[0]);
			default:
				return new ListTag();
		}
	}

	private static String require(String command) {
		if (!fits(command)) {
			throw new IllegalArgumentException("too_long");
		}
		return command;
	}
}
