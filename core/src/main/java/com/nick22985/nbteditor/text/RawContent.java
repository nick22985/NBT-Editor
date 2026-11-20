package com.nick22985.nbteditor.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record RawContent(String kind, Map<String, Object> fields) implements Content {
	public RawContent {
		fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
	}

	@Override
	public String plain() {
		return "";
	}
}
