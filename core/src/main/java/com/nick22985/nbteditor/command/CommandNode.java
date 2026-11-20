package com.nick22985.nbteditor.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandNode {
	private final String name;
	private final ArgSpec argument;
	private final List<CommandNode> children = new ArrayList<>();
	private CommandExecutor executor;
	private BooleanSupplier requirement = () -> true;
	private CommandNode redirect;

	private CommandNode(String name, ArgSpec argument) {
		this.name = name;
		this.argument = argument;
	}

	public static CommandNode literal(String name) {
		return new CommandNode(name, null);
	}

	public static CommandNode argument(String name, ArgSpec argument) {
		return new CommandNode(name, argument);
	}

	public String name() {
		return this.name;
	}

	public boolean isLiteral() {
		return this.argument == null;
	}

	public ArgSpec spec() {
		return this.argument;
	}

	public List<CommandNode> children() {
		return Collections.unmodifiableList(this.children);
	}

	public CommandExecutor executor() {
		return this.executor;
	}

	public BooleanSupplier requirement() {
		return this.requirement;
	}

	public CommandNode redirectTarget() {
		return this.redirect;
	}

	public CommandNode then(CommandNode child) {
		this.children.add(child);
		return this;
	}

	public CommandNode executes(CommandExecutor executor) {
		this.executor = executor;
		return this;
	}

	public CommandNode requires(BooleanSupplier requirement) {
		this.requirement = requirement;
		return this;
	}

	public CommandNode redirect(CommandNode target) {
		this.redirect = target;
		return this;
	}

	public CommandNode child(String name) {
		for (CommandNode child : this.children) {
			if (child.name.equals(name)) {
				return child;
			}
		}
		return null;
	}

	public CommandNode renamed(String name) {
		CommandNode copy = new CommandNode(name, this.argument);
		copy.children.addAll(this.children);
		copy.executor = this.executor;
		copy.requirement = this.requirement;
		copy.redirect = this.redirect;
		return copy;
	}
}
