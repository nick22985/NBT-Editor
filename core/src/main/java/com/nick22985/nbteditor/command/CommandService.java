package com.nick22985.nbteditor.command;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface CommandService {
	void register(CommandNode root);

	void send(String command);

	boolean serverKnows(String root);

	List<String> serverChildren(String root);

	void execute(String command);
}
