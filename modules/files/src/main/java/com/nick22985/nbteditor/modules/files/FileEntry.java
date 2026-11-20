package com.nick22985.nbteditor.modules.files;

import java.nio.file.Path;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record FileEntry(Path path, String name, boolean directory, long size, NbtFileFormat format, boolean image) {
	public boolean openable() {
		return this.directory || this.format != null;
	}
}
