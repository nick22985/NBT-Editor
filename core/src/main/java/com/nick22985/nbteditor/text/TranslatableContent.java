package com.nick22985.nbteditor.text;

import java.util.Collections;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record TranslatableContent(String key, String fallback, List<Object> args) implements Content {
	public TranslatableContent {
		args = args == null ? List.of() : Collections.unmodifiableList(args);
	}

	@Override
	public String kind() {
		return "translatable";
	}

	@Override
	public String plain() {
		return fallback != null ? fallback : key;
	}
}
