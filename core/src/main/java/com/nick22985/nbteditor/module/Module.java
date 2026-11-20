package com.nick22985.nbteditor.module;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Module {
	String id();

	default List<String> dependencies() {
		return List.of();
	}

	default List<String> optionalDependencies() {
		return List.of();
	}

	default boolean enabledByDefault() {
		return true;
	}

	default boolean canDisable() {
		return true;
	}

	default boolean clientOnly() {
		return false;
	}

	void init(ModuleContext ctx);

	default void ready(ModuleContext ctx) {
	}

	default void shutdown(ModuleContext ctx) {
	}
}
