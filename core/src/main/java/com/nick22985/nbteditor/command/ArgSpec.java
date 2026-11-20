package com.nick22985.nbteditor.command;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ArgSpec(ArgKind kind, double min, double max, SuggestionProvider suggestions) {
	public static ArgSpec word() {
		return new ArgSpec(ArgKind.WORD, 0, 0, null);
	}

	public static ArgSpec string() {
		return new ArgSpec(ArgKind.STRING, 0, 0, null);
	}

	public static ArgSpec id() {
		return new ArgSpec(ArgKind.ID, 0, 0, null);
	}

	public static ArgSpec greedy() {
		return new ArgSpec(ArgKind.GREEDY, 0, 0, null);
	}

	public static ArgSpec integer(int min, int max) {
		return new ArgSpec(ArgKind.INTEGER, min, max, null);
	}

	public static ArgSpec integer() {
		return integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static ArgSpec decimal(double min, double max) {
		return new ArgSpec(ArgKind.DECIMAL, min, max, null);
	}

	public static ArgSpec bool() {
		return new ArgSpec(ArgKind.BOOLEAN, 0, 0, null);
	}

	public static ArgSpec choice(List<String> values) {
		List<String> copy = List.copyOf(values);
		return new ArgSpec(ArgKind.WORD, 0, 0, (context, remaining) -> copy);
	}

	public ArgSpec suggest(SuggestionProvider suggestions) {
		return new ArgSpec(this.kind, this.min, this.max, suggestions);
	}

	public ArgSpec suggest(Supplier<List<String>> suggestions) {
		return this.suggest((context, remaining) -> suggestions.get());
	}
}
