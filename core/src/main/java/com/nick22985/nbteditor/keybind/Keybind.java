package com.nick22985.nbteditor.keybind;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.KeyInput;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Keybind {
	KeybindSpec spec();

	boolean isDown();

	boolean consumeClick();

	boolean matches(KeyInput input);

	Text boundKey();
}
