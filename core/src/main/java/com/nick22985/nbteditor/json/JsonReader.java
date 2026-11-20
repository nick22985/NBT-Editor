package com.nick22985.nbteditor.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class JsonReader {
	private final String input;
	private final boolean lenient;
	private int cursor;

	public JsonReader(String input, boolean lenient) {
		this.input = input;
		this.lenient = lenient;
	}

	public Object read() throws JsonException {
		skip();
		Object value = value();
		skip();
		if (cursor < input.length()) {
			throw error("Unexpected trailing data");
		}
		return value;
	}

	private JsonException error(String message) {
		return new JsonException(message, cursor);
	}

	private void skip() {
		while (cursor < input.length()) {
			char c = input.charAt(cursor);
			if (Character.isWhitespace(c)) {
				cursor++;
			} else if (lenient && c == '/' && cursor + 1 < input.length() && input.charAt(cursor + 1) == '/') {
				while (cursor < input.length() && input.charAt(cursor) != '\n') {
					cursor++;
				}
			} else if (lenient && c == '/' && cursor + 1 < input.length() && input.charAt(cursor + 1) == '*') {
				int end = input.indexOf("*/", cursor + 2);
				cursor = end < 0 ? input.length() : end + 2;
			} else {
				break;
			}
		}
	}

	private Object value() throws JsonException {
		if (cursor >= input.length()) {
			throw error("Expected value");
		}
		char c = input.charAt(cursor);
		switch (c) {
			case '{':
				return object();
			case '[':
				return array();
			case '"':
				return string('"');
			case '\'':
				if (lenient) {
					return string('\'');
				}
				throw error("Unexpected '");
			default:
				return literal();
		}
	}

	private Map<String, Object> object() throws JsonException {
		cursor++;
		Map<String, Object> map = new LinkedHashMap<>();
		skip();
		if (peekIs('}')) {
			cursor++;
			return map;
		}
		while (true) {
			skip();
			if (lenient && peekIs('}')) {
				cursor++;
				return map;
			}
			String key;
			if (peekIs('"')) {
				key = string('"');
			} else if (lenient && peekIs('\'')) {
				key = string('\'');
			} else if (lenient) {
				key = bare();
			} else {
				throw error("Expected key");
			}
			skip();
			if (!peekIs(':') && !(lenient && peekIs('='))) {
				throw error("Expected ':'");
			}
			cursor++;
			skip();
			map.put(key, value());
			skip();
			if (peekIs(',') || lenient && peekIs(';')) {
				cursor++;
				continue;
			}
			if (peekIs('}')) {
				cursor++;
				return map;
			}
			throw error("Expected ',' or '}'");
		}
	}

	private List<Object> array() throws JsonException {
		cursor++;
		List<Object> list = new ArrayList<>();
		skip();
		if (peekIs(']')) {
			cursor++;
			return list;
		}
		while (true) {
			skip();
			if (lenient && peekIs(']')) {
				cursor++;
				return list;
			}
			list.add(value());
			skip();
			if (peekIs(',') || lenient && peekIs(';')) {
				cursor++;
				continue;
			}
			if (peekIs(']')) {
				cursor++;
				return list;
			}
			throw error("Expected ',' or ']'");
		}
	}

	private boolean peekIs(char c) {
		return cursor < input.length() && input.charAt(cursor) == c;
	}

	private String string(char quote) throws JsonException {
		cursor++;
		StringBuilder out = new StringBuilder();
		while (cursor < input.length()) {
			char c = input.charAt(cursor++);
			if (c == quote) {
				return out.toString();
			}
			if (c != '\\') {
				out.append(c);
				continue;
			}
			if (cursor >= input.length()) {
				break;
			}
			char e = input.charAt(cursor++);
			switch (e) {
				case 'b':
					out.append('\b');
					break;
				case 'f':
					out.append('\f');
					break;
				case 'n':
					out.append('\n');
					break;
				case 'r':
					out.append('\r');
					break;
				case 't':
					out.append('\t');
					break;
				case 'u':
					if (cursor + 4 > input.length()) {
						throw error("Bad unicode escape");
					}
					try {
						out.append((char) Integer.parseInt(input.substring(cursor, cursor + 4), 16));
					} catch (NumberFormatException ex) {
						throw error("Bad unicode escape");
					}
					cursor += 4;
					break;
				default:
					out.append(e);
					break;
			}
		}
		throw error("Unterminated string");
	}

	private String bare() throws JsonException {
		int start = cursor;
		while (cursor < input.length()) {
			char c = input.charAt(cursor);
			if (Character.isWhitespace(c) || ",:=;{}[]\"'".indexOf(c) >= 0) {
				break;
			}
			cursor++;
		}
		if (cursor == start) {
			throw error("Expected value");
		}
		return input.substring(start, cursor);
	}

	private Object literal() throws JsonException {
		int start = cursor;
		String token = bare();
		switch (token) {
			case "true":
				return Boolean.TRUE;
			case "false":
				return Boolean.FALSE;
			case "null":
				return JsonNull.INSTANCE;
			default:
				break;
		}
		try {
			if (token.matches("-?(?:0|[1-9][0-9]*)")) {
				long value = Long.parseLong(token);
				if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
					return (int) value;
				}
				return value;
			}
			if (token.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?")) {
				return Double.parseDouble(token);
			}
			if (lenient) {
				try {
					return new BigDecimal(token).doubleValue();
				} catch (NumberFormatException ignored) {
					return token;
				}
			}
		} catch (NumberFormatException ignored) {
		}
		cursor = start;
		throw error("Unexpected token '" + token + "'");
	}
}
