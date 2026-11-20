package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.ui.Rect;
import com.nick22985.nbteditor.ui.tab.TabPosition;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TabUtil {
	private static final int EDGE = 10;
	private static final int SPACING = 2;

	private TabUtil() {
	}

	public static List<Rect> layout(TabPosition position, int screenWidth, int screenHeight, int count, int tabWidth, int tabHeight) {
		List<Rect> rects = new ArrayList<>(count);
		if (position.hidden() || count <= 0) {
			return rects;
		}
		int step = tabWidth + SPACING;
		int total = count * step - SPACING;
		int x;
		if (position.align() < 0) {
			x = EDGE;
		} else if (position.align() > 0) {
			x = screenWidth - EDGE - total;
		} else {
			x = (screenWidth - total) / 2;
		}
		int y = position.top() ? 0 : screenHeight - tabHeight;
		for (int i = 0; i < count; i++) {
			rects.add(new Rect(x + i * step, y, tabWidth, tabHeight));
		}
		return rects;
	}
}
