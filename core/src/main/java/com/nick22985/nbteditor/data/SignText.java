package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record SignText(List<Text> lines, String color, boolean glowing) {
	public static final int LINES = 4;
	public static final SignText EMPTY = new SignText(List.of(Text.EMPTY, Text.EMPTY, Text.EMPTY, Text.EMPTY), DyeColor.BLACK.id(), false);

	public SignText withLine(int index, Text line) {
		List<Text> next = new ArrayList<>(this.lines);
		next.set(index, line);
		return new SignText(next, this.color, this.glowing);
	}

	public SignText withColor(String color) {
		return new SignText(this.lines, color, this.glowing);
	}

	public SignText withGlowing(boolean glowing) {
		return new SignText(this.lines, this.color, glowing);
	}
}
