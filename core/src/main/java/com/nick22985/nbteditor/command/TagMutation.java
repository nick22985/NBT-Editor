package com.nick22985.nbteditor.command;

import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface TagMutation {
	void apply(CompoundTag tag) throws CommandException;
}
