package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.doc.DocumentEvent;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.KeyInput;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface EditorPane {
	void build(EditorFrame frame);

	default boolean hasSide() {
		return true;
	}

	default int sideTopHeight() {
		return 0;
	}

	default boolean showsPreview() {
		return true;
	}

	default Text hint() {
		return null;
	}

	default void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
	}

	default void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
	}

	default boolean keyPressed(KeyInput input) {
		return false;
	}

	default void documentChanged(DocumentEvent event) {
	}

	default void focusDefault() {
	}

	default void tick() {
	}

	default void removed() {
	}
}
