package com.nick22985.nbteditor.module;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum ModuleState {
	ENABLED,
	DISABLED,
	MISSING_DEPENDENCY,
	DEPENDENCY_DISABLED,
	CYCLE,
	FAILED,
	SKIPPED
}
