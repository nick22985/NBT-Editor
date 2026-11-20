package com.nick22985.nbteditor.command;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface SuggestionProvider {
	List<String> suggest(CommandContext context, String remaining);
}
