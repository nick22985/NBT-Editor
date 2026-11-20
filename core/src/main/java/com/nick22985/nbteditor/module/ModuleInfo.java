package com.nick22985.nbteditor.module;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ModuleInfo(Module module, ModuleState state, String reason) {
	public String id() {
		return this.module.id();
	}

	public boolean enabled() {
		return this.state == ModuleState.ENABLED;
	}
}
