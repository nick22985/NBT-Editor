package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.json.JsonReader;
import com.nick22985.nbteditor.json.JsonWriter;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class JsonUtil {
	private JsonUtil() {
	}

	public static Object parse(String input) throws JsonException {
		return new JsonReader(input, false).read();
	}

	public static Object parse(String input, boolean lenient) throws JsonException {
		return new JsonReader(input, lenient).read();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseObject(String input) throws JsonException {
		Object value = parse(input, true);
		if (!(value instanceof Map)) {
			throw new JsonException("Expected a JSON object", 0);
		}
		return (Map<String, Object>) value;
	}

	public static String write(Object value) {
		return JsonWriter.COMPACT.write(value);
	}

	public static String pretty(Object value) {
		return JsonWriter.PRETTY.write(value);
	}

	public static String quote(String value) {
		StringBuilder out = new StringBuilder();
		JsonWriter.COMPACT.quote(value, out);
		return out.toString();
	}
}
