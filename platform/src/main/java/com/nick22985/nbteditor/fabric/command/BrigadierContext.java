package com.nick22985.nbteditor.fabric.command;

import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.text.Text;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BrigadierContext<S> implements CommandContext {
	private final com.mojang.brigadier.context.CommandContext<S> context;
	private final BiConsumer<S, Component> feedback;
	private final BiConsumer<S, Component> error;

	public BrigadierContext(com.mojang.brigadier.context.CommandContext<S> context, BiConsumer<S, Component> feedback, BiConsumer<S, Component> error) {
		this.context = context;
		this.feedback = feedback;
		this.error = error;
	}

	private <T> T argument(String name, Class<T> type) {
		return this.context.getArgument(name, type);
	}

	@Override
	public boolean has(String name) {
		try {
			this.context.getArgument(name, Object.class);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public String string(String name) {
		return this.argument(name, String.class);
	}

	@Override
	public int integer(String name) {
		return this.argument(name, Integer.class);
	}

	@Override
	public double decimal(String name) {
		return this.argument(name, Double.class);
	}

	@Override
	public boolean bool(String name) {
		return this.argument(name, Boolean.class);
	}

	@Override
	public String input() {
		return this.context.getInput();
	}

	@Override
	public void feedback(Text text) {
		this.feedback.accept(this.context.getSource(), McTextUtil.toMc(text));
	}

	@Override
	public void error(Text text) {
		this.error.accept(this.context.getSource(), McTextUtil.toMc(text));
	}
}
