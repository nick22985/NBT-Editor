package com.nick22985.nbteditor.data;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ArmorTrim(String material, String pattern) {
	public ArmorTrim withMaterial(String material) {
		return new ArmorTrim(material, this.pattern);
	}

	public ArmorTrim withPattern(String pattern) {
		return new ArmorTrim(this.material, pattern);
	}
}
