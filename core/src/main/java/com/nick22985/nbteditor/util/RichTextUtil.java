package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.LiteralContent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.text.RichCell;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RichTextUtil {
	private RichTextUtil() {
	}

	public static List<RichCell> cells(Text text) {
		List<RichCell> out = new ArrayList<>();
		if (text != null) {
			collect(text, Style.EMPTY, out);
		}
		return out;
	}

	public static List<RichCell> cells(List<Text> lines) {
		List<RichCell> out = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				out.add(RichCell.of('\n', Style.EMPTY));
			}
			collect(lines.get(i), Style.EMPTY, out);
		}
		return out;
	}

	private static void collect(Text text, Style parent, List<RichCell> out) {
		Style resolved = text.style().inheritFrom(parent);
		if (text.content() instanceof LiteralContent) {
			String value = ((LiteralContent) text.content()).text();
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c != '\r') {
					out.add(RichCell.of(c, resolved));
				}
			}
		} else {
			out.add(RichCell.atom(text.content(), resolved));
		}
		for (Text sibling : text.siblings()) {
			collect(sibling, resolved, out);
		}
	}

	public static String plain(List<RichCell> cells) {
		StringBuilder out = new StringBuilder(cells.size());
		for (RichCell cell : cells) {
			out.append(cell.ch());
		}
		return out.toString();
	}

	public static Text build(List<RichCell> cells) {
		return build(cells, 0, cells.size());
	}

	public static Text build(List<RichCell> cells, int from, int to) {
		List<Text> parts = new ArrayList<>();
		StringBuilder run = new StringBuilder();
		Style runStyle = null;
		for (int i = from; i < to; i++) {
			RichCell cell = cells.get(i);
			if (cell.isAtom() || runStyle != null && !runStyle.equals(cell.style())) {
				if (run.length() > 0) {
					parts.add(Text.literal(run.toString(), runStyle));
					run.setLength(0);
				}
				runStyle = null;
			}
			if (cell.isAtom()) {
				parts.add(Text.of(cell.atom(), cell.style(), List.of()));
				continue;
			}
			runStyle = cell.style();
			run.append(cell.ch());
		}
		if (run.length() > 0) {
			parts.add(Text.literal(run.toString(), runStyle));
		}
		if (parts.isEmpty()) {
			return Text.EMPTY;
		}
		if (parts.size() == 1) {
			return parts.get(0);
		}
		Style common = parts.get(0).style();
		for (Text part : parts) {
			common = common.common(part.style());
		}
		List<Text> children = new ArrayList<>(parts.size());
		for (Text part : parts) {
			children.add(part.withStyle(part.style().without(common)));
		}
		return Text.of(LiteralContent.EMPTY, common, children);
	}

	public static List<Text> lines(List<RichCell> cells) {
		List<Text> out = new ArrayList<>();
		int start = 0;
		for (int i = 0; i <= cells.size(); i++) {
			if (i == cells.size() || cells.get(i).isNewline()) {
				out.add(build(cells, start, i));
				start = i + 1;
			}
		}
		return out;
	}

	public static boolean same(Text a, Text b) {
		return cells(a).equals(cells(b));
	}

	public static boolean same(List<Text> a, List<Text> b) {
		return cells(a).equals(cells(b));
	}

	public static Text placeholder(RichCell cell) {
		return Text.literal("[" + cell.atom().kind() + "]", cell.style());
	}

	public static Text display(RichCell cell) {
		if (cell.isAtom()) {
			return Text.of(cell.atom(), cell.style(), List.of());
		}
		return Text.literal(String.valueOf(cell.ch()), cell.style());
	}
}
