package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Canvas extends TextMetrics {
	int width();

	int height();

	float partialTick();

	void fill(int x1, int y1, int x2, int y2, int color);

	void gradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom);

	void text(String text, int x, int y, int color, boolean shadow);

	void text(Text text, int x, int y, int color, boolean shadow);

	void sprite(String id, int x, int y, int width, int height);

	void texture(String id, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight);

	void button(int x, int y, int width, int height, boolean hovered, boolean enabled);

	void slot(int x, int y);

	void slotIcon(SlotIcon icon, int x, int y);

	void item(GameItem item, int x, int y, float scale, boolean decorations);

	void tooltip(List<Text> lines, int x, int y);

	void itemTooltip(GameItem item, int x, int y);

	void pushClip(int x, int y, int width, int height);

	void popClip();

	void pushOffset(int dx, int dy);

	void popOffset();

	void nextLayer();

	default void item(GameItem item, int x, int y) {
		this.item(item, x, y, 1.0F, true);
	}
}
