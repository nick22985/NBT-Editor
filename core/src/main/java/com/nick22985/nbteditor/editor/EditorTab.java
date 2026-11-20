package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface EditorTab {
	RegistryKey<EditorTab> REGISTRY = RegistryKey.of("editor_tab", EditorTab.class);

	String id();

	Text label();

	default boolean applies(EditorSession session) {
		return true;
	}

	default Text unavailable(EditorSession session) {
		return null;
	}

	EditorPane create(EditorSession session);
}
