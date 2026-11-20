package com.nick22985.nbteditor.fabric.client;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.nick22985.nbteditor.fabric.compat.NbtacCompletions;
import com.nick22985.nbteditor.platform.NbtCompletionService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.suggest.TextSuggestion;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricNbtCompletionService implements NbtCompletionService {
	private final boolean loaded = NbtacCompletions.SUPPORTED && FabricLoader.getInstance().isModLoaded("nbtac");
	private NbtacCompletions nbtac;

	@Override
	public boolean available() {
		return this.loaded;
	}

	private NbtacCompletions nbtac() {
		if (this.nbtac == null) {
			this.nbtac = new NbtacCompletions();
		}
		return this.nbtac;
	}

	@Override
	public CompletableFuture<List<TextSuggestion>> nbt(String name, String snbt) {
		return this.convert(() -> this.nbtac().nbt(name, snbt));
	}

	@Override
	public CompletableFuture<List<TextSuggestion>> component(String itemId, String component, String snbt) {
		return this.convert(() -> this.nbtac().component(itemId, component, snbt));
	}

	private CompletableFuture<List<TextSuggestion>> convert(Supplier<CompletableFuture<Suggestions>> source) {
		if (!this.loaded) {
			return CompletableFuture.completedFuture(List.of());
		}
		CompletableFuture<Suggestions> future;
		try {
			future = source.get();
		} catch (RuntimeException e) {
			return CompletableFuture.completedFuture(List.of());
		}
		return future.handle((suggestions, failure) -> {
			List<TextSuggestion> out = new ArrayList<>();
			if (failure != null || suggestions == null) {
				return out;
			}
			for (Suggestion suggestion : suggestions.getList()) {
				Text tooltip = suggestion.getTooltip() == null ? null : Text.literal(suggestion.getTooltip().getString());
				out.add(new TextSuggestion(suggestion.getRange().getStart(), suggestion.getRange().getEnd(), suggestion.getText(), tooltip));
			}
			return out;
		});
	}
}
