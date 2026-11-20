package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.LongTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.SnbtWriter;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.nbt.TagType;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TagDisplayUtil {
	private static final int INLINE_LIMIT = 256;

	private TagDisplayUtil() {
	}

	public static List<TagType> menuTypes() {
		return List.of(TagType.COMPOUND, TagType.LIST, TagType.STRING, TagType.BYTE, TagType.SHORT, TagType.INT, TagType.LONG, TagType.FLOAT, TagType.DOUBLE, TagType.BYTE_ARRAY, TagType.INT_ARRAY, TagType.LONG_ARRAY);
	}

	public static String chip(TagType type) {
		switch (type) {
			case COMPOUND:
				return "{}";
			case LIST:
				return "[]";
			case BYTE_ARRAY:
				return "[B";
			case INT_ARRAY:
				return "[I";
			case LONG_ARRAY:
				return "[L";
			case BYTE:
				return "B";
			case SHORT:
				return "S";
			case INT:
				return "I";
			case LONG:
				return "L";
			case FLOAT:
				return "F";
			case DOUBLE:
				return "D";
			case STRING:
				return "T";
			default:
				return "?";
		}
	}

	public static int chipColor(TagType type) {
		if (type == TagType.STRING) {
			return Theme.GREEN;
		}
		if (type.isNumeric() || type.isArray()) {
			return Theme.GOLD;
		}
		return Theme.TEXT;
	}

	public static Text typeName(TagType type) {
		return Text.translatable("nbtedit.tag.type." + type.name().toLowerCase(Locale.ROOT));
	}

	public static Text summary(Tag tag) {
		if (tag instanceof CompoundTag) {
			int size = ((CompoundTag) tag).size();
			return Text.translatable(size == 1 ? "nbtedit.tag.key" : "nbtedit.tag.keys", size);
		}
		int size = TagArrayUtil.containerSize(tag);
		return Text.translatable(size == 1 ? "nbtedit.tag.item" : "nbtedit.tag.items", size);
	}

	public static Text inline(Tag tag, SnbtDialect dialect) {
		if (tag instanceof NumericTag) {
			return SnbtHighlightUtil.number(SnbtWriter.compact(dialect).number((NumericTag) tag));
		}
		if (tag instanceof StringTag) {
			String value = ((StringTag) tag).value();
			if (value.length() > INLINE_LIMIT) {
				value = value.substring(0, INLINE_LIMIT) + "…";
			}
			return TextStyleUtil.colored(SnbtWriter.compact(dialect).quote(value), Theme.GREEN);
		}
		return SnbtHighlightUtil.highlight(SnbtUtil.write(tag, dialect), INLINE_LIMIT, dialect);
	}

	public static String editText(Tag tag, SnbtDialect dialect) {
		if (tag instanceof StringTag) {
			return ((StringTag) tag).value();
		}
		if (tag instanceof NumericTag) {
			return SnbtWriter.compact(dialect).number((NumericTag) tag);
		}
		return SnbtUtil.write(tag, dialect);
	}

	public static Tag parseEdit(String text, Tag current, SnbtDialect dialect) throws SnbtException {
		if (current instanceof StringTag) {
			return StringTag.of(text);
		}
		Tag parsed = SnbtUtil.parse(text.trim(), dialect, true);
		if (current instanceof NumericTag && parsed instanceof NumericTag && !hasSuffix(text.trim())) {
			return NumericTag.of(current.type(), ((NumericTag) parsed).number());
		}
		return parsed;
	}

	private static boolean hasSuffix(String text) {
		if (text.isEmpty()) {
			return false;
		}
		char last = Character.toLowerCase(text.charAt(text.length() - 1));
		return last == 'b' || last == 's' || last == 'l' || last == 'f' || last == 'd';
	}

	public static Tag defaultTag(TagType type) {
		switch (type) {
			case COMPOUND:
				return new CompoundTag();
			case LIST:
				return new ListTag();
			case STRING:
				return StringTag.EMPTY;
			case BYTE_ARRAY:
				return new ByteArrayTag(new byte[0]);
			case INT_ARRAY:
				return new IntArrayTag(new int[0]);
			case LONG_ARRAY:
				return new LongArrayTag(new long[0]);
			default:
				return NumericTag.of(type, 0);
		}
	}

	public static Tag convert(Tag tag, TagType to, SnbtDialect dialect) {
		if (tag.type() == to) {
			return tag.copy();
		}
		if (to.isNumeric()) {
			if (tag instanceof NumericTag) {
				return NumericTag.of(to, ((NumericTag) tag).number());
			}
			if (tag instanceof StringTag) {
				Double value = NumberUtil.parseDouble(((StringTag) tag).value().trim());
				return NumericTag.of(to, value == null ? 0 : value);
			}
			return defaultTag(to);
		}
		if (to == TagType.STRING) {
			return StringTag.of(editText(tag, dialect));
		}
		if (tag instanceof StringTag) {
			try {
				Tag parsed = SnbtUtil.parse(((StringTag) tag).value(), dialect, true);
				return parsed.type() == to ? parsed : convert(parsed, to, dialect);
			} catch (SnbtException e) {
				return defaultTag(to);
			}
		}
		List<Tag> elements = elements(tag);
		switch (to) {
			case LIST:
				return new ListTag(elements);
			case COMPOUND: {
				CompoundTag compound = new CompoundTag();
				for (int i = 0; i < elements.size(); i++) {
					compound.put(Integer.toString(i), elements.get(i));
				}
				return compound;
			}
			case BYTE_ARRAY: {
				List<NumericTag> numbers = numbers(elements);
				byte[] out = new byte[numbers.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = numbers.get(i).byteValue();
				}
				return new ByteArrayTag(out);
			}
			case INT_ARRAY: {
				List<NumericTag> numbers = numbers(elements);
				int[] out = new int[numbers.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = numbers.get(i).intValue();
				}
				return new IntArrayTag(out);
			}
			case LONG_ARRAY: {
				List<NumericTag> numbers = numbers(elements);
				long[] out = new long[numbers.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = numbers.get(i).longValue();
				}
				return new LongArrayTag(out);
			}
			default:
				return defaultTag(to);
		}
	}

	public static List<Tag> elements(Tag tag) {
		List<Tag> out = new ArrayList<>();
		if (tag instanceof CompoundTag) {
			for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
				out.add(entry.getValue().copy());
			}
		} else if (tag instanceof ListTag) {
			for (Tag element : (ListTag) tag) {
				out.add(element.copy());
			}
		} else if (tag instanceof ByteArrayTag) {
			for (byte value : ((ByteArrayTag) tag).value()) {
				out.add(ByteTag.of(value));
			}
		} else if (tag instanceof IntArrayTag) {
			for (int value : ((IntArrayTag) tag).value()) {
				out.add(IntTag.of(value));
			}
		} else if (tag instanceof LongArrayTag) {
			for (long value : ((LongArrayTag) tag).value()) {
				out.add(LongTag.of(value));
			}
		} else if (tag instanceof NumericTag || tag instanceof StringTag) {
			out.add(tag.copy());
		}
		return out;
	}

	private static List<NumericTag> numbers(List<Tag> elements) {
		List<NumericTag> out = new ArrayList<>();
		for (Tag element : elements) {
			if (element instanceof NumericTag) {
				out.add((NumericTag) element);
			}
		}
		return out;
	}

	public static Tag childOf(Tag container, int index) {
		if (container instanceof ListTag) {
			return ((ListTag) container).get(index);
		}
		if (container instanceof ByteArrayTag) {
			return ByteTag.of(((ByteArrayTag) container).get(index));
		}
		if (container instanceof IntArrayTag) {
			return IntTag.of(((IntArrayTag) container).get(index));
		}
		if (container instanceof LongArrayTag) {
			return LongTag.of(((LongArrayTag) container).get(index));
		}
		return null;
	}

	public static Tag elementTemplate(Tag container) {
		if (container instanceof ByteArrayTag) {
			return ByteTag.ZERO;
		}
		if (container instanceof IntArrayTag) {
			return IntTag.of(0);
		}
		if (container instanceof LongArrayTag) {
			return LongTag.of(0);
		}
		if (container instanceof ListTag && !((ListTag) container).isEmpty()) {
			return defaultTag(((ListTag) container).get(0).type());
		}
		return null;
	}

	public static boolean matches(String query, String text) {
		return text.toLowerCase(Locale.ROOT).contains(query);
	}
}
