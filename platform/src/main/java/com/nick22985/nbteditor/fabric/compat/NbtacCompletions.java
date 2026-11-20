package com.nick22985.nbteditor.fabric.compat;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
//? if nbtac {
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import java.util.function.Function;
import net.mt1006.nbtac.api.NBTacAPI;
//?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtacCompletions {
	//? if nbtac {
	public static final boolean SUPPORTED = true;
	//?} else
	//public static final boolean SUPPORTED = false;

	public CompletableFuture<Suggestions> nbt(String name, String snbt) {
		//? if nbtac {
		return NBTacAPI.getNbtSuggestions(snbt, name, new SuggestionsBuilder(snbt, 0), false, Function.identity());
		//?} else
		//return new SuggestionsBuilder(snbt, 0).buildFuture();
	}

	public CompletableFuture<Suggestions> component(String itemId, String component, String snbt) {
		//? if nbtac {
		return NBTacAPI.getItemDataSuggestions(snbt, "item/" + component, McIdUtil.parse(itemId), null, Function.identity());
		//?} else
		//return new SuggestionsBuilder(snbt, 0).buildFuture();
	}
}
