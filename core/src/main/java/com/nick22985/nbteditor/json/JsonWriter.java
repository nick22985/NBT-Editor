package com.nick22985.nbteditor.json;

import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class JsonWriter {
	public static final JsonWriter COMPACT = new JsonWriter(null);
	public static final JsonWriter PRETTY = new JsonWriter("  ");

	private final String indent;

	public JsonWriter(String indent) {
		this.indent = indent;
	}

	public String write(Object value) {
		StringBuilder out = new StringBuilder();
		write(value, out, 0);
		return out.toString();
	}

	@SuppressWarnings("unchecked")
	private void write(Object value, StringBuilder out, int depth) {
		if (value == null || value == JsonNull.INSTANCE) {
			out.append("null");
		} else if (value instanceof String) {
			quote((String) value, out);
		} else if (value instanceof Boolean) {
			out.append(value);
		} else if (value instanceof Double || value instanceof Float) {
			double d = ((Number) value).doubleValue();
			if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
				out.append((long) d).append(".0");
			} else {
				out.append(d);
			}
		} else if (value instanceof Number) {
			out.append(value);
		} else if (value instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) value;
			if (map.isEmpty()) {
				out.append("{}");
				return;
			}
			out.append('{');
			boolean first = true;
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				if (!first) {
					out.append(',');
				}
				first = false;
				newline(out, depth + 1);
				quote(entry.getKey(), out);
				out.append(indent != null ? ": " : ":");
				write(entry.getValue(), out, depth + 1);
			}
			newline(out, depth);
			out.append('}');
		} else if (value instanceof List) {
			List<Object> list = (List<Object>) value;
			if (list.isEmpty()) {
				out.append("[]");
				return;
			}
			out.append('[');
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) {
					out.append(',');
				}
				newline(out, depth + 1);
				write(list.get(i), out, depth + 1);
			}
			newline(out, depth);
			out.append(']');
		} else {
			quote(value.toString(), out);
		}
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

	public void quote(String value, StringBuilder out) {
		out.append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"':
					out.append("\\\"");
					break;
				case '\\':
					out.append("\\\\");
					break;
				case '\n':
					out.append("\\n");
					break;
				case '\r':
					out.append("\\r");
					break;
				case '\t':
					out.append("\\t");
					break;
				case '\b':
					out.append("\\b");
					break;
				case '\f':
					out.append("\\f");
					break;
				default:
					if (c < 0x20 || c == 0x2028 || c == 0x2029) {
						out.append(String.format("\\u%04x", (int) c));
					} else {
						out.append(c);
					}
			}
		}
		out.append('"');
	}
}
