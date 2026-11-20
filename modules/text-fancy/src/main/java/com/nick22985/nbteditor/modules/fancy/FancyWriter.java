package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.Content;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.HoverEvent;
import com.nick22985.nbteditor.text.KeybindContent;
import com.nick22985.nbteditor.text.NbtContent;
import com.nick22985.nbteditor.text.ScoreContent;
import com.nick22985.nbteditor.text.SelectorContent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.text.TranslatableContent;
import com.nick22985.nbteditor.ui.text.RichCell;
import com.nick22985.nbteditor.util.RichTextUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.List;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FancyWriter {
	private final FancyGrammar grammar;
	private final StringBuilder out = new StringBuilder();
	private List<RichCell> cells;
	private String plain;
	private Style state = Style.EMPTY;
	private boolean afterHex;

	public FancyWriter(FancyGrammar grammar) {
		this.grammar = grammar;
	}

	public String write(Text text) {
		return this.write(RichTextUtil.cells(text));
	}

	public String write(List<RichCell> cells) {
		this.cells = cells;
		this.plain = RichTextUtil.plain(cells);
		this.out.setLength(0);
		this.state = Style.EMPTY;
		this.afterHex = false;
		int i = 0;
		while (i < cells.size()) {
			Style style = cells.get(i).style();
			int end = i + 1;
			while (end < cells.size() && this.sameEvents(cells.get(end).style(), style)) {
				end++;
			}
			if (this.hasEvents(style)) {
				if (!this.sameFormatting(this.state, Style.EMPTY)) {
					this.out.append("&r");
					this.state = Style.EMPTY;
					this.afterHex = false;
				}
				int groups = this.openGroups(style);
				this.writeCells(i, end, groups > 0);
				for (int k = 0; k < groups; k++) {
					this.out.append(')');
				}
				if (groups > 0) {
					this.state = Style.EMPTY;
					this.afterHex = false;
				}
			} else {
				this.writeCells(i, end, false);
			}
			i = end;
		}
		return this.out.toString();
	}

	private boolean hasEvents(Style style) {
		return style.clickEvent() != null || style.hoverEvent() != null || style.insertion() != null || style.font() != null;
	}

	private boolean sameEvents(Style a, Style b) {
		return Objects.equals(a.clickEvent(), b.clickEvent()) && Objects.equals(a.hoverEvent(), b.hoverEvent()) && Objects.equals(a.insertion(), b.insertion()) && Objects.equals(a.font(), b.font());
	}

	private int openGroups(Style style) {
		int groups = 0;
		ClickEvent click = style.clickEvent();
		if (click != null && click.value() != null && this.grammar.isOption(click.action().serializedName())) {
			this.openGroup(click.action().serializedName(), this.escapeValue(click.value(), false));
			groups++;
		}
		String hover = this.hover(style.hoverEvent());
		if (hover != null) {
			this.openGroup(style.hoverEvent().action().serializedName(), hover);
			groups++;
		}
		if (style.insertion() != null) {
			this.openGroup("insertion", this.escapeValue(style.insertion(), false));
			groups++;
		}
		if (style.font() != null) {
			this.openGroup("font", this.escapeValue(style.font(), false));
			groups++;
		}
		return groups;
	}

	private String hover(HoverEvent hover) {
		if (hover == null) {
			return null;
		}
		switch (hover.action()) {
			case SHOW_TEXT:
				return this.escapeValue(new FancyWriter(this.grammar).write(hover.text()), false);
			case SHOW_ITEM:
				String item = hover.id() + " " + hover.count();
				if (hover.data() != null && !hover.data().isEmpty()) {
					item += " " + SnbtUtil.write(hover.data(), SnbtDialect.MODERN);
				}
				return this.escapeValue(item, false);
			case SHOW_ENTITY:
				String entity = this.escapeValue(hover.id(), false) + " " + hover.uuid();
				if (hover.text() != null) {
					entity += " " + this.escapeValue(new FancyWriter(this.grammar).write(hover.text()), false);
				}
				return entity;
			default:
				return null;
		}
	}

	private void openGroup(String name, String value) {
		this.out.append('[').append(name).append("]{").append(value).append("}(");
	}

	private String escapeValue(String value, boolean pipes) {
		StringBuilder escaped = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\' || c == '{' || c == '}' || pipes && c == '|') {
				escaped.append('\\');
			}
			escaped.append(c);
		}
		return escaped.toString();
	}

	private void writeCells(int from, int to, boolean grouped) {
		for (int k = from; k < to; k++) {
			RichCell cell = this.cells.get(k);
			this.transition(cell.style());
			if (cell.isAtom()) {
				this.out.append(this.atom(cell.atom()));
				this.afterHex = false;
				continue;
			}
			this.writeChar(cell.ch(), k, grouped);
			this.afterHex = false;
		}
	}

	private void writeChar(char c, int index, boolean grouped) {
		if (c == '\r') {
			return;
		}
		boolean escape;
		switch (c) {
			case '\\':
				escape = true;
				break;
			case '&':
			case Formatting.SECTION:
				escape = index + 1 < this.plain.length() && this.grammar.formsCode(this.plain.charAt(index + 1));
				break;
			case '[':
				escape = this.grammar.optionAt(this.plain, index) != null;
				break;
			case '<':
				escape = this.grammar.tagAt(this.plain, index) != null;
				break;
			case ')':
				escape = grouped;
				break;
			case ';':
				escape = this.afterHex;
				break;
			default:
				escape = false;
		}
		if (escape) {
			this.out.append('\\');
		}
		this.out.append(c);
	}

	private String atom(Content content) {
		if (content instanceof TranslatableContent) {
			TranslatableContent translatable = (TranslatableContent) content;
			StringBuilder value = new StringBuilder(this.escapeValue(translatable.key(), true));
			for (Object arg : translatable.args()) {
				String text = arg instanceof Text ? new FancyWriter(this.grammar).write((Text) arg) : String.valueOf(arg);
				value.append('|').append(this.escapeValue(text, true));
			}
			return "[translate]{" + value + "}";
		}
		if (content instanceof KeybindContent) {
			return "[keybind]{" + this.escapeValue(((KeybindContent) content).key(), false) + "}";
		}
		if (content instanceof ScoreContent) {
			ScoreContent score = (ScoreContent) content;
			return "[score]{" + this.escapeValue(score.name(), true) + "|" + this.escapeValue(score.objective(), true) + "}";
		}
		if (content instanceof SelectorContent) {
			return "[selector]{" + this.escapeValue(((SelectorContent) content).pattern(), false) + "}";
		}
		if (content instanceof NbtContent) {
			NbtContent nbt = (NbtContent) content;
			String value = this.escapeValue(nbt.sourceKind(), true) + "|" + this.escapeValue(nbt.source(), true) + "|" + this.escapeValue(nbt.path(), true);
			return "[nbt]{" + value + (nbt.interpret() ? "|interpret" : "") + "}";
		}
		return "";
	}

	private void transition(Style to) {
		Style from = this.state;
		if (this.sameFormatting(from, to)) {
			return;
		}
		boolean shadowRemoved = from.shadowColor() != null && to.shadowColor() == null;
		boolean reduced = this.removesFlag(from, to) || from.color() != null && to.color() == null;
		Style current = from;
		if (shadowRemoved || reduced && to.color() == null) {
			this.out.append("&r");
			this.afterHex = false;
			current = Style.EMPTY;
		}
		if (to.color() != null && (!Objects.equals(current.color(), to.color()) || this.removesFlag(current, to))) {
			this.color(to.color());
			current = Style.EMPTY.withColor(to.color()).withShadowColor(current.shadowColor());
		}
		this.flag(current.isObfuscated(), to.isObfuscated(), Formatting.OBFUSCATED);
		this.flag(current.isBold(), to.isBold(), Formatting.BOLD);
		this.flag(current.isStrikethrough(), to.isStrikethrough(), Formatting.STRIKETHROUGH);
		this.flag(current.isUnderlined(), to.isUnderlined(), Formatting.UNDERLINE);
		this.flag(current.isItalic(), to.isItalic(), Formatting.ITALIC);
		if (to.shadowColor() != null && !Objects.equals(current.shadowColor(), to.shadowColor())) {
			this.out.append("&_#").append(String.format("%08X", to.shadowColor())).append(';');
			this.afterHex = false;
		}
		this.state = to;
	}

	private void color(TextColor color) {
		if (color.named() != null) {
			this.out.append('&').append(color.named().code());
			this.afterHex = false;
		} else {
			this.out.append("&#").append(String.format("%06X", color.rgb()));
			this.afterHex = true;
		}
	}

	private void flag(boolean from, boolean to, Formatting formatting) {
		if (to && !from) {
			this.out.append('&').append(formatting.code());
			this.afterHex = false;
		}
	}

	private boolean removesFlag(Style from, Style to) {
		return from.isBold() && !to.isBold() || from.isItalic() && !to.isItalic() || from.isUnderlined() && !to.isUnderlined() || from.isStrikethrough() && !to.isStrikethrough() || from.isObfuscated() && !to.isObfuscated();
	}

	private boolean sameFormatting(Style a, Style b) {
		return Objects.equals(a.color(), b.color()) && Objects.equals(a.shadowColor(), b.shadowColor()) && a.isBold() == b.isBold() && a.isItalic() == b.isItalic() && a.isUnderlined() == b.isUnderlined() && a.isStrikethrough() == b.isStrikethrough() && a.isObfuscated() == b.isObfuscated();
	}
}
