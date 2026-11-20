package com.nick22985.nbteditor.command;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface CommandContext {
	boolean has(String name);

	String string(String name);

	int integer(String name);

	double decimal(String name);

	boolean bool(String name);

	String input();

	void feedback(Text text);

	void error(Text text);
}
