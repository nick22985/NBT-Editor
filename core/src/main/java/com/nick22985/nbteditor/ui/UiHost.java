package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface UiHost extends TextMetrics {
	int screenWidth();

	int screenHeight();

	String clipboard();

	void setClipboard(String text);

	void playClick();

	void open(UiScreen screen);

	void replace(UiScreen screen);

	void close(UiScreen screen);

	boolean isKeyDown(int key);

	Modifiers modifiers();

	long millis();

	String translate(String key, Object... args);

	boolean hasTranslation(String key);

	void toast(Text title, Text message);
}
