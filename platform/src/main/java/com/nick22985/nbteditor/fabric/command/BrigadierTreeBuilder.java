package com.nick22985.nbteditor.fabric.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandExecutor;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.command.SuggestionProvider;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BrigadierTreeBuilder<S> {
	private final BiConsumer<S, Component> feedback;
	private final BiConsumer<S, Component> error;
	private final Map<CommandNode, com.mojang.brigadier.tree.CommandNode<S>> built = new IdentityHashMap<>();
	private final Set<CommandNode> building = new HashSet<>();

	public BrigadierTreeBuilder(BiConsumer<S, Component> feedback, BiConsumer<S, Component> error) {
		this.feedback = feedback;
		this.error = error;
	}

	public LiteralArgumentBuilder<S> root(CommandNode node) {
		LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.literal(node.name());
		this.fill(node, builder);
		return builder;
	}

	public void remember(CommandNode node, com.mojang.brigadier.tree.CommandNode<S> result) {
		this.built.put(node, result);
	}

	private ArgumentBuilder<S, ?> builder(CommandNode node) {
		if (node.isLiteral()) {
			return LiteralArgumentBuilder.literal(node.name());
		}
		ArgSpec spec = node.spec();
		RequiredArgumentBuilder<S, ?> builder = RequiredArgumentBuilder.argument(node.name(), type(spec));
		SuggestionProvider suggestions = spec.suggestions();
		if (suggestions != null) {
			builder.suggests((context, suggestionsBuilder) -> this.suggest(suggestions, context, suggestionsBuilder));
		}
		return builder;
	}

	private static ArgumentType<?> type(ArgSpec spec) {
		switch (spec.kind()) {
			case STRING:
				return StringArgumentType.string();
			case GREEDY:
				return StringArgumentType.greedyString();
			case INTEGER:
				return IntegerArgumentType.integer((int) Math.max(Integer.MIN_VALUE, spec.min()), (int) Math.min(Integer.MAX_VALUE, spec.max()));
			case DECIMAL:
				return DoubleArgumentType.doubleArg(spec.min(), spec.max());
			case BOOLEAN:
				return BoolArgumentType.bool();
			case ID:
				return new IdArgumentType();
			default:
				return StringArgumentType.word();
		}
	}

	private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(SuggestionProvider provider, com.mojang.brigadier.context.CommandContext<S> context, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining();
		String lower = remaining.toLowerCase(Locale.ROOT);
		List<String> values;
		try {
			values = provider.suggest(new BrigadierContext<>(context, this.feedback, this.error), remaining);
		} catch (RuntimeException e) {
			values = List.of();
		}
		if (values != null) {
			for (String value : values) {
				String candidate = value.toLowerCase(Locale.ROOT);
				int colon = candidate.indexOf(':');
				if (candidate.startsWith(lower) || colon >= 0 && candidate.startsWith(lower, colon + 1)) {
					builder.suggest(value);
				}
			}
		}
		return builder.buildFuture();
	}

	private int run(CommandExecutor executor, com.mojang.brigadier.context.CommandContext<S> context) {
		try {
			executor.run(new BrigadierContext<>(context, this.feedback, this.error));
			return 1;
		} catch (CommandException e) {
			this.error.accept(context.getSource(), McTextUtil.toMc(e.text()));
			return 0;
		}
	}

	private void fill(CommandNode node, ArgumentBuilder<S, ?> builder) {
		this.building.add(node);
		builder.requires(source -> node.requirement().getAsBoolean());
		CommandNode target = node.redirectTarget();
		com.mojang.brigadier.tree.CommandNode<S> redirect = target == null ? null : this.built.get(target);
		if (redirect != null) {
			builder.redirect(redirect);
		} else {
			CommandNode source = target != null && !this.building.contains(target) ? target : node;
			CommandExecutor executor = node.executor() != null ? node.executor() : source.executor();
			if (executor != null) {
				builder.executes(context -> this.run(executor, context));
			}
			for (CommandNode child : source.children()) {
				this.child(builder, child);
			}
			if (source != node) {
				for (CommandNode child : node.children()) {
					this.child(builder, child);
				}
			}
		}
		this.building.remove(node);
	}

	private void child(ArgumentBuilder<S, ?> parent, CommandNode child) {
		ArgumentBuilder<S, ?> builder = this.builder(child);
		this.fill(child, builder);
		com.mojang.brigadier.tree.CommandNode<S> result = builder.build();
		this.built.put(child, result);
		parent.then(result);
	}
}
