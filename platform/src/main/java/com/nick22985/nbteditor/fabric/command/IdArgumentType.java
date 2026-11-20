package com.nick22985.nbteditor.fabric.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class IdArgumentType implements ArgumentType<String> {
	private static final List<String> EXAMPLES = List.of("stone", "minecraft:diamond_sword", "custom:path/name");

	@Override
	public String parse(StringReader reader) {
		int start = reader.getCursor();
		while (reader.canRead() && this.allowed(reader.peek())) {
			reader.skip();
		}
		return reader.getString().substring(start, reader.getCursor());
	}

	private boolean allowed(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == ':' || c == '/';
	}

	@Override
	public List<String> getExamples() {
		return EXAMPLES;
	}
}
