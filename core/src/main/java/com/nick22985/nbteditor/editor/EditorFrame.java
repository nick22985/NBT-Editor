package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Button;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface EditorFrame {
	EditorSession session();

	UiScreen screen();

	int bodyX();

	int bodyY();

	int bodyWidth();

	int bodyHeight();

	int mainWidth();

	int sideX();

	int sideWidth();

	<T extends Widget> T add(T widget);

	Button footerButton(Text label, Runnable onPress);

	void status(Text status);

	void rebuild();
}
