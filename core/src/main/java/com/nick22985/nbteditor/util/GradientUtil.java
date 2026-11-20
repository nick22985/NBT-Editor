package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.ui.text.RichCell;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GradientUtil {
	private GradientUtil() {
	}

	public static int at(int[] stops, float progress) {
		if (stops.length == 0) {
			return 0xFFFFFF;
		}
		if (stops.length == 1) {
			return stops[0];
		}
		float scaled = Math.max(0, Math.min(1, progress)) * (stops.length - 1);
		int index = Math.min(stops.length - 2, (int) Math.floor(scaled));
		return ColorUtil.lerp(stops[index], stops[index + 1], scaled - index);
	}

	public static List<Style> styles(List<RichCell> cells, int[] stops) {
		int visible = 0;
		for (RichCell cell : cells) {
			if (!cell.isNewline()) {
				visible++;
			}
		}
		List<Style> out = new ArrayList<>(cells.size());
		int index = 0;
		for (RichCell cell : cells) {
			if (cell.isNewline()) {
				out.add(cell.style());
				continue;
			}
			float progress = visible <= 1 ? 0 : index / (float) (visible - 1);
			out.add(cell.style().withColor(TextColor.exactRgb(at(stops, progress))));
			index++;
		}
		return out;
	}

	public static Text apply(Text text, int[] stops) {
		List<RichCell> cells = RichTextUtil.cells(text);
		List<Style> styles = styles(cells, stops);
		List<RichCell> out = new ArrayList<>(cells.size());
		for (int i = 0; i < cells.size(); i++) {
			out.add(cells.get(i).withStyle(styles.get(i)));
		}
		return RichTextUtil.build(out);
	}

	public static int[] parseStops(String value) {
		List<Integer> stops = new ArrayList<>();
		for (String part : value.trim().split("[\\s,:]+")) {
			if (part.isEmpty()) {
				continue;
			}
			Integer rgb = ColorUtil.parseHex(part);
			if (rgb == null) {
				TextColor named = TextColor.parse(part);
				if (named == null) {
					return null;
				}
				rgb = named.rgb();
			}
			stops.add(rgb);
		}
		if (stops.isEmpty()) {
			return null;
		}
		return stops.stream().mapToInt(Integer::intValue).toArray();
	}

	public static int[] rainbow(int count, float phase) {
		int size = Math.max(2, count);
		int[] out = new int[size];
		for (int i = 0; i < size; i++) {
			out[i] = ColorUtil.hsvToRgb((phase + i / (float) size) % 1.0F, 1.0F, 1.0F);
		}
		return out;
	}
}
