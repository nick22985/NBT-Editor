package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.ui.suggest.TextSuggestion;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface NbtCompletionService {
	boolean available();

	CompletableFuture<List<TextSuggestion>> nbt(String name, String snbt);

	CompletableFuture<List<TextSuggestion>> component(String itemId, String component, String snbt);
}
