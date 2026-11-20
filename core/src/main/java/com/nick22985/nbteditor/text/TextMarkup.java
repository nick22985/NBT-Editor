package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.module.RegistryKey;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface TextMarkup {
	RegistryKey<TextMarkup> REGISTRY = RegistryKey.of("text_markup", TextMarkup.class);

	Text apply(String value, Text contents);
}
