package com.nick22985.nbteditor.fabric.util;

import net.minecraft.resources.Identifier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McIdUtil {
	private McIdUtil() {
	}

	public static Identifier of(String namespace, String path) {
		//? if >=1.21 {
		return Identifier.fromNamespaceAndPath(namespace, path);
		//?} else
		//return new Identifier(namespace, path);
	}

	public static Identifier parse(String id) {
		Identifier parsed = Identifier.tryParse(id);
		if (parsed == null) {
			throw new IllegalArgumentException("Bad id " + id);
		}
		return parsed;
	}

	public static Identifier tryParse(String id) {
		return id == null ? null : Identifier.tryParse(id);
	}
}
