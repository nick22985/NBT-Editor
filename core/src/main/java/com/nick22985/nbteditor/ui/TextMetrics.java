package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.text.Text;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface TextMetrics {
	int textWidth(String text);

	int textWidth(Text text);

	default int lineHeight() {
		return 9;
	}

	List<Text> wrap(Text text, int width);

	String trimToWidth(String text, int width);

	Text trimToWidth(Text text, int width);
}
