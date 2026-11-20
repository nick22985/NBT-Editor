package com.nick22985.nbteditor.nbt;

import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SnbtWriter {
	private final SnbtDialect dialect;
	private final String indent;
	private final boolean spaced;
	private final boolean sortKeys;
	private final boolean gameSafe;

	private SnbtWriter(SnbtDialect dialect, String indent, boolean spaced, boolean sortKeys, boolean gameSafe) {
		this.dialect = dialect;
		this.indent = indent;
		this.spaced = spaced;
		this.sortKeys = sortKeys;
		this.gameSafe = gameSafe;
	}

	public static SnbtWriter compact() {
		return new SnbtWriter(SnbtDialect.MODERN, null, false, false, false);
	}

	public static SnbtWriter compact(SnbtDialect dialect) {
		return new SnbtWriter(dialect, null, false, false, false);
	}

	public static SnbtWriter pretty(SnbtDialect dialect) {
		return new SnbtWriter(dialect, "  ", true, false, false);
	}

	public static SnbtWriter forCommands(SnbtDialect dialect) {
		return new SnbtWriter(dialect, null, false, false, true);
	}

	public SnbtWriter withIndent(String indent) {
		return new SnbtWriter(dialect, indent, spaced, sortKeys, gameSafe);
	}

	public SnbtWriter withSpaces(boolean spaced) {
		return new SnbtWriter(dialect, indent, spaced, sortKeys, gameSafe);
	}

	public SnbtWriter sorted(boolean sortKeys) {
		return new SnbtWriter(dialect, indent, spaced, sortKeys, gameSafe);
	}

	public SnbtWriter gameSafe(boolean gameSafe) {
		return new SnbtWriter(dialect, indent, spaced, sortKeys, gameSafe);
	}

	public SnbtDialect dialect() {
		return dialect;
	}

	public String write(Tag tag) {
		StringBuilder out = new StringBuilder();
		write(tag, out, 0);
		return out.toString();
	}

	private void write(Tag tag, StringBuilder out, int depth) {
		switch (tag.type()) {
			case COMPOUND:
				writeCompound((CompoundTag) tag, out, depth);
				break;
			case LIST:
				writeList((ListTag) tag, out, depth);
				break;
			case STRING:
				out.append(quote(((StringTag) tag).value()));
				break;
			case BYTE_ARRAY: {
				byte[] values = ((ByteArrayTag) tag).value();
				out.append("[B;");
				for (int i = 0; i < values.length; i++) {
					separator(out, i);
					out.append(values[i]).append('B');
				}
				out.append(']');
				break;
			}
			case INT_ARRAY: {
				int[] values = ((IntArrayTag) tag).value();
				out.append("[I;");
				for (int i = 0; i < values.length; i++) {
					separator(out, i);
					out.append(values[i]);
				}
				out.append(']');
				break;
			}
			case LONG_ARRAY: {
				long[] values = ((LongArrayTag) tag).value();
				out.append("[L;");
				for (int i = 0; i < values.length; i++) {
					separator(out, i);
					out.append(values[i]).append('L');
				}
				out.append(']');
				break;
			}
			case END:
				break;
			default:
				out.append(number((NumericTag) tag));
				break;
		}
	}

	private void separator(StringBuilder out, int index) {
		if (index > 0) {
			out.append(spaced ? ", " : ",");
		}
	}

	private void writeCompound(CompoundTag compound, StringBuilder out, int depth) {
		if (compound.isEmpty()) {
			out.append("{}");
			return;
		}
		List<String> keys = new ArrayList<>(compound.keys());
		if (sortKeys) {
			keys.sort(null);
		}
		out.append('{');
		for (int i = 0; i < keys.size(); i++) {
			if (i > 0) {
				out.append(indent != null ? "," : spaced ? ", " : ",");
			}
			newline(out, depth + 1);
			String key = keys.get(i);
			out.append(key(key)).append(spaced || indent != null ? ": " : ":");
			write(compound.get(key), out, depth + 1);
		}
		newline(out, depth);
		out.append('}');
	}

	private void writeList(ListTag list, StringBuilder out, int depth) {
		if (list.isEmpty()) {
			out.append("[]");
			return;
		}
		boolean multiline = indent != null && list.elements().stream().anyMatch(tag -> tag.type() == TagType.COMPOUND || tag.type() == TagType.LIST);
		out.append('[');
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				out.append(multiline ? "," : spaced || indent != null ? ", " : ",");
			}
			if (multiline) {
				newline(out, depth + 1);
			}
			write(list.get(i), out, depth + 1);
		}
		if (multiline) {
			newline(out, depth);
		}
		out.append(']');
	}

	private void newline(StringBuilder out, int depth) {
		if (indent == null) {
			return;
		}
		out.append('\n');
		for (int i = 0; i < depth; i++) {
			out.append(indent);
		}
	}

	public String number(NumericTag tag) {
		switch (tag.type()) {
			case BYTE:
				return tag.byteValue() + "b";
			case SHORT:
				return tag.shortValue() + "s";
			case INT:
				return Integer.toString(tag.intValue());
			case LONG:
				return tag.longValue() + "L";
			case FLOAT: {
				float value = tag.floatValue();
				if (gameSafe && Float.isInfinite(value)) {
					return (value < 0 ? "-" : "") + "1.0e1000f";
				}
				return value + "f";
			}
			default: {
				double value = tag.doubleValue();
				if (gameSafe && Double.isInfinite(value)) {
					return (value < 0 ? "-" : "") + "1.0e1000d";
				}
				return value + "d";
			}
		}
	}

	public String key(String key) {
		if (!key.isEmpty() && isUnquoted(key)) {
			return key;
		}
		return quote(key);
	}

	private boolean isUnquoted(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (!SnbtUtil.isUnquotedChar(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public String quote(String value) {
		StringBuilder out = new StringBuilder(value.length() + 2);
		out.append(' ');
		char quote = 0;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\') {
				out.append('\\');
			} else if (c == '"' || c == '\'') {
				if (quote == 0) {
					quote = c == '"' ? '\'' : '"';
				}
				if (quote == c) {
					out.append('\\');
				}
			} else if (dialect == SnbtDialect.MODERN && c < 0x20) {
				switch (c) {
					case '\b':
						out.append("\\b");
						continue;
					case '\t':
						out.append("\\t");
						continue;
					case '\n':
						out.append("\\n");
						continue;
					case '\f':
						out.append("\\f");
						continue;
					case '\r':
						out.append("\\r");
						continue;
					default:
						out.append(String.format("\\x%02x", (int) c));
						continue;
				}
			}
			out.append(c);
		}
		if (quote == 0) {
			quote = '"';
		}
		out.setCharAt(0, quote);
		out.append(quote);
		return out.toString();
	}
}
