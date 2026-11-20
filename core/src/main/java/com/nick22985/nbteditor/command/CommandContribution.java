package com.nick22985.nbteditor.command;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record CommandContribution(String group, CommandNode node, Text description, String alias) {
	public static final RegistryKey<CommandContribution> REGISTRY = RegistryKey.of("command", CommandContribution.class);
	public static final String ROOT = "";
	public static final String NBT = "nbt";
	public static final String GET = "get";
	public static final String FACTORY = "factory";

	public String id() {
		return this.group.isEmpty() ? this.node.name() : this.group + " " + this.node.name();
	}

	public boolean root() {
		return this.group.isEmpty();
	}
}
