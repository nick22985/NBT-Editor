package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.nbt.NbtPath;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record NbtSuggestSubject(String type, String id, boolean components, NbtPath dataPath) {
	public String name() {
		return this.type + "/" + this.id;
	}
}
