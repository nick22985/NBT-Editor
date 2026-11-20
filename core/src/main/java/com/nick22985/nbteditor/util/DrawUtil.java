package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DrawUtil {
	private static final String ELLIPSIS = "...";

	private DrawUtil() {
	}

	public static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static void rect(Canvas canvas, int x, int y, int width, int height, int color) {
		if (width > 0 && height > 0) {
			canvas.fill(x, y, x + width, y + height, color);
		}
	}

	public static void outline(Canvas canvas, int x, int y, int width, int height, int color) {
		rect(canvas, x, y, width, 1, color);
		rect(canvas, x, y + height - 1, width, 1, color);
		rect(canvas, x, y + 1, 1, height - 2, color);
		rect(canvas, x + width - 1, y + 1, 1, height - 2, color);
	}

	public static void dashedOutline(Canvas canvas, int x, int y, int width, int height, int color) {
		for (int i = 0; i < width; i += 4) {
			int dash = Math.min(2, width - i);
			rect(canvas, x + i, y, dash, 1, color);
			rect(canvas, x + i, y + height - 1, dash, 1, color);
		}
		for (int i = 0; i < height; i += 4) {
			int dash = Math.min(2, height - i);
			rect(canvas, x, y + i, 1, dash, color);
			rect(canvas, x + width - 1, y + i, 1, dash, color);
		}
	}

	public static void well(Canvas canvas, int x, int y, int width, int height) {
		rect(canvas, x, y, width, height, Theme.WELL);
		outline(canvas, x, y, width, height, Theme.WELL_BORDER);
	}

	public static void modalFrame(Canvas canvas, int x, int y, int width, int height) {
		outline(canvas, x - 1, y - 1, width + 2, height + 2, Theme.FIELD);
		rect(canvas, x, y, width, height, Theme.MODAL);
		outline(canvas, x, y, width, height, Theme.MODAL_BORDER);
	}

	public static void progressBar(Canvas canvas, int x, int y, int width, int height, float progress) {
		field(canvas, x, y, width, height, Theme.FIELD_BORDER);
		rect(canvas, x + 1, y + 1, Math.round((width - 2) * Math.max(0, Math.min(1, progress))), height - 2, Theme.PROGRESS);
	}

	public static void busyBar(Canvas canvas, int x, int y, int width, int height, long millis) {
		field(canvas, x, y, width, height, Theme.FIELD_BORDER);
		int inner = width - 2;
		int segment = Math.max(1, inner / 4);
		int offset = (int) (millis / 8 % (inner + segment)) - segment;
		int start = Math.max(0, offset);
		int end = Math.min(inner, offset + segment);
		if (end > start) {
			rect(canvas, x + 1 + start, y + 1, end - start, height - 2, Theme.PROGRESS);
		}
	}

	public static void containerPanel(Canvas canvas, int x, int y, int width, int height) {
		rect(canvas, x + 2, y, width - 4, height, 0xFF000000);
		rect(canvas, x, y + 2, width, height - 4, 0xFF000000);
		rect(canvas, x + 1, y + 1, width - 2, height - 2, 0xFF000000);
		rect(canvas, x + 2, y + 1, width - 4, height - 2, 0xFFC6C6C6);
		rect(canvas, x + 1, y + 2, width - 2, height - 4, 0xFFC6C6C6);
		rect(canvas, x + 2, y + 1, width - 5, 2, 0xFFFFFFFF);
		rect(canvas, x + 1, y + 2, 2, height - 5, 0xFFFFFFFF);
		rect(canvas, x + 3, y + 3, 1, 1, 0xFFFFFFFF);
		rect(canvas, x + 3, y + height - 3, width - 5, 2, 0xFF555555);
		rect(canvas, x + width - 3, y + 3, 2, height - 5, 0xFF555555);
		rect(canvas, x + width - 4, y + height - 4, 1, 1, 0xFF555555);
	}

	public static void separator(Canvas canvas, int x, int y, int width) {
		rect(canvas, x, y, width, 1, Theme.SEPARATOR_LIGHT);
		rect(canvas, x, y + 1, width, 1, Theme.SEPARATOR_DARK);
	}

	public static void sectionHeading(Canvas canvas, Text label, int x, int y, int width) {
		int labelWidth = textClipped(canvas, label, x, y + 1, width, Theme.LABEL);
		int lineX = x + labelWidth + Theme.GAP;
		if (lineX < x + width) {
			rect(canvas, lineX, y + 5, x + width - lineX, 1, Theme.SECTION_LINE);
		}
	}

	public static void field(Canvas canvas, int x, int y, int width, int height, int borderColor) {
		rect(canvas, x, y, width, height, Theme.FIELD);
		outline(canvas, x, y, width, height, borderColor);
	}

	public static int spriteWidth(String[] rows) {
		return rows.length == 0 ? 0 : rows[0].length();
	}

	public static int spriteHeight(String[] rows) {
		return rows.length;
	}

	public static void sprite(Canvas canvas, String[] rows, int x, int y, int color) {
		for (int row = 0; row < rows.length; row++) {
			String line = rows[row];
			int start = -1;
			for (int col = 0; col <= line.length(); col++) {
				boolean filled = col < line.length() && line.charAt(col) != '.';
				if (filled && start < 0) {
					start = col;
				} else if (!filled && start >= 0) {
					rect(canvas, x + start, y + row, col - start, 1, color);
					start = -1;
				}
			}
		}
	}

	public static void spriteCentered(Canvas canvas, String[] rows, int x, int y, int width, int height, int color) {
		sprite(canvas, rows, x + (width - spriteWidth(rows)) / 2, y + (height - spriteHeight(rows)) / 2, color);
	}

	public static void text(Canvas canvas, Text text, int x, int y, int color) {
		canvas.text(text, x, y, color, true);
	}

	public static void text(Canvas canvas, String text, int x, int y, int color) {
		canvas.text(text, x, y, color, true);
	}

	public static void textCentered(Canvas canvas, Text text, int centerX, int y, int color) {
		canvas.text(text, centerX - canvas.textWidth(text) / 2, y, color, true);
	}

	public static int textClipped(Canvas canvas, Text text, int x, int y, int maxWidth, int color) {
		if (maxWidth <= 0) {
			return 0;
		}
		int width = canvas.textWidth(text);
		if (width <= maxWidth) {
			canvas.text(text, x, y, color, true);
			return width;
		}
		int ellipsisWidth = canvas.textWidth(ELLIPSIS);
		Text cut = canvas.trimToWidth(text, Math.max(0, maxWidth - ellipsisWidth));
		canvas.text(cut, x, y, color, true);
		int cutWidth = canvas.textWidth(cut);
		canvas.text(ELLIPSIS, x + cutWidth, y, Theme.LABEL, true);
		return cutWidth + ellipsisWidth;
	}

	public static int textClipped(Canvas canvas, String text, int x, int y, int maxWidth, int color) {
		if (maxWidth <= 0) {
			return 0;
		}
		int width = canvas.textWidth(text);
		if (width <= maxWidth) {
			canvas.text(text, x, y, color, true);
			return width;
		}
		int ellipsisWidth = canvas.textWidth(ELLIPSIS);
		String cut = canvas.trimToWidth(text, Math.max(0, maxWidth - ellipsisWidth));
		canvas.text(cut, x, y, color, true);
		int cutWidth = canvas.textWidth(cut);
		canvas.text(ELLIPSIS, x + cutWidth, y, Theme.LABEL, true);
		return cutWidth + ellipsisWidth;
	}

	public static int maxTextWidth(TextMetrics metrics, List<Text> values) {
		int max = 0;
		for (Text value : values) {
			max = Math.max(max, metrics.textWidth(value));
		}
		return max;
	}

	public static int chip(Canvas canvas, String label, int x, int y, int color) {
		int width = chipWidth(canvas, label);
		rect(canvas, x, y, width, 10, Theme.CHIP);
		outline(canvas, x, y, width, 10, Theme.CHIP_BORDER);
		canvas.text(label, x + (width - canvas.textWidth(label) + 1) / 2, y + 1, color, true);
		return width;
	}

	public static int chipWidth(TextMetrics metrics, String label) {
		return Math.max(13, metrics.textWidth(label) + 3);
	}

	public static void tooltipFrame(Canvas canvas, int x, int y, int width, int height) {
		rect(canvas, x + 1, y, width - 2, height, Theme.TOOLTIP);
		rect(canvas, x, y + 1, 1, height - 2, Theme.TOOLTIP);
		rect(canvas, x + width - 1, y + 1, 1, height - 2, Theme.TOOLTIP);
		canvas.gradient(x + 1, y + 2, x + 2, y + height - 2, Theme.TOOLTIP_BORDER_TOP, Theme.TOOLTIP_BORDER_BOTTOM);
		canvas.gradient(x + width - 2, y + 2, x + width - 1, y + height - 2, Theme.TOOLTIP_BORDER_TOP, Theme.TOOLTIP_BORDER_BOTTOM);
		rect(canvas, x + 1, y + 1, width - 2, 1, Theme.TOOLTIP_BORDER_TOP);
		rect(canvas, x + 1, y + height - 2, width - 2, 1, Theme.TOOLTIP_BORDER_BOTTOM);
	}

	public static void tooltipBox(Canvas canvas, List<Text> lines, int x, int y, int maxWidth, int maxHeight) {
		if (lines.isEmpty() || maxWidth < 16 || maxHeight < 16) {
			return;
		}
		int innerMax = maxWidth - 8;
		int width = 0;
		int height = 0;
		for (int i = 0; i < lines.size(); i++) {
			for (Text part : canvas.wrap(lines.get(i), innerMax)) {
				width = Math.max(width, canvas.textWidth(part));
				height += 10;
			}
			if (i == 0 && lines.size() > 1) {
				height += 2;
			}
		}
		int frameWidth = Math.min(maxWidth, width + 8);
		int frameHeight = Math.min(maxHeight, height + 6);
		canvas.pushClip(x, y, frameWidth, frameHeight);
		tooltipFrame(canvas, x, y, frameWidth, Math.max(height + 6, frameHeight));
		int lineY = y + 4;
		for (int i = 0; i < lines.size(); i++) {
			for (Text part : canvas.wrap(lines.get(i), innerMax)) {
				canvas.text(part, x + 4, lineY, Theme.TEXT, true);
				lineY += 10;
			}
			if (i == 0) {
				lineY += 2;
			}
		}
		canvas.popClip();
	}

	public static void scrollbar(Canvas canvas, int x, int y, int height, int contentHeight, int scroll) {
		if (contentHeight <= height || height <= 0) {
			return;
		}
		int thumbHeight = Math.max(8, height * height / contentHeight);
		int thumbY = y + (int) ((long) (height - thumbHeight) * scroll / (contentHeight - height));
		int w = Theme.SCROLLBAR_WIDTH;
		rect(canvas, x, thumbY, w, thumbHeight, Theme.SCROLLBAR_DARK);
		rect(canvas, x, thumbY, w - 1, thumbHeight - 1, Theme.SCROLLBAR_LIGHT);
		rect(canvas, x + 1, thumbY + 1, w - 2, thumbHeight - 2, Theme.SCROLLBAR);
	}

	public static void slotHighlight(Canvas canvas, int x, int y, int color) {
		rect(canvas, x, y, 16, 16, color);
	}

	public static int withAlpha(int color, int alpha) {
		return (alpha & 0xFF) << 24 | color & 0xFFFFFF;
	}

	public static void horizontalGradient(Canvas canvas, int x, int y, int width, int height, int colorLeft, int colorRight) {
		for (int i = 0; i < width; i++) {
			float progress = width <= 1 ? 0 : i / (float) (width - 1);
			int alpha = Math.round((colorLeft >>> 24) + ((colorRight >>> 24) - (colorLeft >>> 24)) * progress);
			canvas.fill(x + i, y, x + i + 1, y + height, withAlpha(ColorUtil.lerp(colorLeft, colorRight, progress), alpha));
		}
	}

	public static void checkerboard(Canvas canvas, int x, int y, int width, int height, int size) {
		for (int row = 0; row * size < height; row++) {
			for (int column = 0; column * size < width; column++) {
				int color = (row + column) % 2 == 0 ? 0xFFBFBFBF : 0xFF7F7F7F;
				int cellX = x + column * size;
				int cellY = y + row * size;
				canvas.fill(cellX, cellY, Math.min(x + width, cellX + size), Math.min(y + height, cellY + size), color);
			}
		}
	}

	public static void swatch(Canvas canvas, int x, int y, int width, int height, int rgb, boolean highlighted) {
		outline(canvas, x, y, width, height, highlighted ? Theme.TEXT : Theme.SWATCH_BORDER);
		rect(canvas, x + 1, y + 1, width - 2, height - 2, ColorUtil.opaque(rgb));
	}
}
