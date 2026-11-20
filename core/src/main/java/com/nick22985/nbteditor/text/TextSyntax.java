package com.nick22985.nbteditor.text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface TextSyntax {
	Text parse(String input);

	String stringify(Text text);

	default boolean recognizes(String input) {
		return input.indexOf('&') >= 0 || input.indexOf(Formatting.SECTION) >= 0;
	}
}
