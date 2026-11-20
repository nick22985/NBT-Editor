package com.nick22985.nbteditor.modules.files;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum NbtFileFormat {
	NBT(Set.of("nbt", "dat", "dat_old", "schem", "schematic", "litematic", "mcstructure")),
	SNBT(Set.of("snbt")),
	REGION(Set.of("mca", "mcr"));

	private final Set<String> extensions;

	NbtFileFormat(Set<String> extensions) {
		this.extensions = extensions;
	}

	public Set<String> extensions() {
		return this.extensions;
	}

	public static NbtFileFormat of(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		int dot = name.lastIndexOf('.');
		if (dot < 0) {
			return null;
		}
		String extension = name.substring(dot + 1);
		for (NbtFileFormat format : values()) {
			if (format.extensions.contains(extension)) {
				return format;
			}
		}
		return null;
	}
}
