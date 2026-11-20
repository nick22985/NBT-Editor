package com.nick22985.nbteditor.ui.text;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.RichTextField;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface RichTextAction {
	RegistryKey<RichTextAction> REGISTRY = RegistryKey.of("rich_text_action", RichTextAction.class);

	Text label();

	default Text tooltip() {
		return null;
	}

	default boolean applies(RichTextField field) {
		return true;
	}

	void run(RichTextField field, Button button);
}
