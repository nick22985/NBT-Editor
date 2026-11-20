package com.nick22985.nbteditor.modules.editor.nbt;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtClipboard {
	private List<ClipEntry> entries = List.of();
	private String text;

	public void set(List<ClipEntry> entries, String text) {
		this.entries = List.copyOf(entries);
		this.text = text;
	}

	public List<ClipEntry> matching(String clipboard) {
		if (this.text == null || clipboard == null || !this.text.equals(clipboard)) {
			return null;
		}
		return this.entries;
	}
}
