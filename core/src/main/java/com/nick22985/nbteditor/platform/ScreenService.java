package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.UiScreen;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ScreenService {
	UiHost host();

	void open(UiScreen screen);

	void closeAll();

	void openInventory();

	boolean anyOpen();

	UiScreen current();

	HoveredItem hovered();

	void toast(Text title, Text message);
}
