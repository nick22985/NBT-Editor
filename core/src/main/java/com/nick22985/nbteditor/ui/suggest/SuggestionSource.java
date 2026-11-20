package com.nick22985.nbteditor.ui.suggest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@FunctionalInterface
public interface SuggestionSource {
	CompletableFuture<List<TextSuggestion>> suggest(String text, int cursor);
}
