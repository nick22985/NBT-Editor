package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class WidgetSizeUtil {
	private WidgetSizeUtil() {
	}

	public static int buttonWidth(TextMetrics metrics, Text label) {
		return Math.max(Theme.MIN_BUTTON_WIDTH, metrics.textWidth(label) + 12);
	}

	public static int checkboxWidth(TextMetrics metrics, Text label) {
		return Checkbox.BOX + 4 + (label == null ? 0 : metrics.textWidth(label));
	}

	public static int buttonRowWidth(TextMetrics metrics, List<Text> labels) {
		int total = 0;
		for (Text label : labels) {
			total += buttonWidth(metrics, label);
		}
		return total + Math.max(0, labels.size() - 1) * Theme.GAP;
	}
}
