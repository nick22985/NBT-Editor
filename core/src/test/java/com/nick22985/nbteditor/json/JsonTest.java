package com.nick22985.nbteditor.json;

import com.nick22985.nbteditor.util.JsonUtil;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class JsonTest {
	@Test
	void strictParse() throws JsonException {
		Object value = JsonUtil.parse("{\"a\":1,\"b\":[true,null,2.5,\"x\\n\"],\"c\":12345678901}");
		Map<?, ?> map = (Map<?, ?>) value;
		assertEquals(1, map.get("a"));
		assertEquals(List.of(true, JsonNull.INSTANCE, 2.5, "x\n"), map.get("b"));
		assertEquals(12345678901L, map.get("c"));
	}

	@Test
	void strictRejectsLenientSyntax() {
		assertThrows(JsonException.class, () -> JsonUtil.parse("{a:1}"));
		assertThrows(JsonException.class, () -> JsonUtil.parse("[1,]"));
		assertThrows(JsonException.class, () -> JsonUtil.parse("{} x"));
	}

	@Test
	void lenientParse() throws JsonException {
		Map<String, Object> map = JsonUtil.parseObject("{ // comment\n a = 'one'; b: bare, c: [1, 2,], }");
		assertEquals("one", map.get("a"));
		assertEquals("bare", map.get("b"));
		assertEquals(List.of(1, 2), map.get("c"));
	}

	@Test
	void writeRoundTrip() throws JsonException {
		String json = "{\"text\":\"a\\\"b\\u2028\",\"n\":[1,2.0,-3.5],\"o\":{}}";
		Object parsed = JsonUtil.parse(json);
		assertEquals(parsed, JsonUtil.parse(JsonUtil.write(parsed)));
		assertEquals(parsed, JsonUtil.parse(JsonUtil.pretty(parsed)));
		assertEquals("\"\\u2028\"", JsonUtil.quote("\u2028"));
	}
}
