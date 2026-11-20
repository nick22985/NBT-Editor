package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.text.ClickAction;
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
import com.nick22985.nbteditor.text.TextMarkup;
import com.nick22985.nbteditor.text.TranslatableContent;
import com.nick22985.nbteditor.ui.text.RichCell;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.RichTextUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FancyParser {
	private final FancyGrammar grammar;
	private final String input;
	private Deque<String> open = new ArrayDeque<>();
	private int pos;

	public FancyParser(FancyGrammar grammar, String input) {
		this.grammar = grammar;
		this.input = input;
	}

	public Text parse() {
		return RichTextUtil.build(this.cells());
	}

	public List<RichCell> cells() {
		this.pos = 0;
		this.open = new ArrayDeque<>();
		return this.sequence(Style.EMPTY, false);
	}

	private List<RichCell> sequence(Style base, boolean grouped) {
		List<RichCell> out = new ArrayList<>();
		Style style = base;
		while (this.pos < this.input.length()) {
			char c = this.input.charAt(this.pos);
			if (c == '\\' && this.pos + 1 < this.input.length() && this.grammar.escapable(this.input.charAt(this.pos + 1))) {
				out.add(RichCell.of(this.input.charAt(this.pos + 1), style));
				this.pos += 2;
				continue;
			}
			if (c == ')' && grouped) {
				return out;
			}
			if (c == '&' || c == Formatting.SECTION) {
				Style next = this.code(style, base);
				if (next != null) {
					style = next;
					continue;
				}
			}
			if (c == '[' && this.group(style, out)) {
				continue;
			}
			if (c == '<') {
				FancyTag tag = this.grammar.tagAt(this.input, this.pos);
				if (tag != null && tag.closing() && this.open.contains(tag.name())) {
					return out;
				}
				if (tag != null && !tag.closing()) {
					style = this.openTag(tag, style, base, grouped, out);
					continue;
				}
			}
			if (c != '\r') {
				out.add(RichCell.of(c, style));
			}
			this.pos++;
		}
		return out;
	}

	private Style code(Style style, Style base) {
		if (this.pos + 1 >= this.input.length()) {
			return null;
		}
		char next = this.input.charAt(this.pos + 1);
		if (next == '#') {
			Integer rgb = this.hex(this.pos + 2, 6);
			if (rgb == null) {
				return null;
			}
			this.pos += 8;
			this.skipSemicolon();
			return this.colored(style, TextColor.rgb(rgb));
		}
		if (next == 'x' || next == 'X') {
			char marker = this.input.charAt(this.pos);
			if (this.pos + 14 > this.input.length()) {
				return null;
			}
			StringBuilder digits = new StringBuilder();
			for (int k = 0; k < 6; k++) {
				int at = this.pos + 2 + k * 2;
				if (this.input.charAt(at) != marker && this.input.charAt(at) != '&' && this.input.charAt(at) != Formatting.SECTION) {
					return null;
				}
				digits.append(this.input.charAt(at + 1));
			}
			Integer rgb = ColorUtil.parseHex(digits.toString());
			if (rgb == null) {
				return null;
			}
			this.pos += 14;
			return this.colored(style, TextColor.rgb(rgb));
		}
		if (next == '_') {
			return this.shadow(style);
		}
		Formatting formatting = Formatting.byCode(next);
		if (formatting == null) {
			return null;
		}
		this.pos += 2;
		return formatting == Formatting.RESET ? base : style.apply(formatting);
	}

	private Style colored(Style style, TextColor color) {
		return style.apply(Formatting.WHITE).withColor(color);
	}

	private Style shadow(Style style) {
		if (this.pos + 2 >= this.input.length()) {
			return null;
		}
		char code = this.input.charAt(this.pos + 2);
		if (code == '#') {
			int start = this.pos + 3;
			int end = start;
			while (end < this.input.length() && end - start < 8 && Character.digit(this.input.charAt(end), 16) >= 0) {
				end++;
			}
			int length = end - start;
			if (length != 6 && length != 8) {
				return null;
			}
			long value = Long.parseLong(this.input.substring(start, end), 16);
			int argb = length == 6 ? ColorUtil.opaque((int) value) : (int) value;
			this.pos = end;
			this.skipSemicolon();
			return style.withShadowColor(argb);
		}
		Formatting formatting = Formatting.byCode(code);
		if (formatting == Formatting.RESET) {
			this.pos += 3;
			return style.withShadowColor(null);
		}
		if (formatting == null || !formatting.isColor()) {
			return null;
		}
		this.pos += 3;
		return style.withShadowColor(ColorUtil.opaque(formatting.rgb()));
	}

	private void skipSemicolon() {
		if (this.pos < this.input.length() && this.input.charAt(this.pos) == ';') {
			this.pos++;
		}
	}

	private Integer hex(int start, int length) {
		if (start + length > this.input.length()) {
			return null;
		}
		for (int k = start; k < start + length; k++) {
			if (Character.digit(this.input.charAt(k), 16) < 0) {
				return null;
			}
		}
		return Integer.parseInt(this.input.substring(start, start + length), 16);
	}

	private Style openTag(FancyTag tag, Style style, Style base, boolean grouped, List<RichCell> out) {
		this.pos += tag.length();
		if (FancyGrammar.RESET.equals(tag.name())) {
			return base;
		}
		if (FancyGrammar.NEWLINE.equals(tag.name())) {
			out.add(RichCell.of('\n', style));
			return style;
		}
		Style inner = this.tagStyle(tag, style);
		this.open.push(tag.name());
		List<RichCell> contents = this.sequence(inner, grouped);
		this.open.pop();
		FancyTag closing = this.grammar.tagAt(this.input, this.pos);
		if (closing != null && closing.closing() && closing.name().equals(tag.name())) {
			this.pos += closing.length();
		}
		TextMarkup markup = this.grammar.markup(tag.name());
		out.addAll(markup == null ? contents : this.applyMarkup(markup, tag.argument(), contents));
		return style;
	}

	private Style tagStyle(FancyTag tag, Style style) {
		switch (tag.name()) {
			case FancyGrammar.COLOR:
				return style.withColor(TextColor.parse(tag.argument()));
			case "bold":
				return style.withBold(true);
			case "italic":
				return style.withItalic(true);
			case "underlined":
				return style.withUnderlined(true);
			case "strikethrough":
				return style.withStrikethrough(true);
			case "obfuscated":
				return style.withObfuscated(true);
			default:
				return style;
		}
	}

	private List<RichCell> applyMarkup(TextMarkup markup, String value, List<RichCell> contents) {
		Text result = markup.apply(value, RichTextUtil.build(contents));
		return result == null ? contents : RichTextUtil.cells(result);
	}

	private boolean group(Style style, List<RichCell> out) {
		int start = this.pos;
		String name = this.grammar.optionAt(this.input, start);
		if (name == null) {
			return false;
		}
		this.pos = start + name.length() + 3;
		String raw = this.value();
		if (raw == null) {
			this.pos = start;
			return false;
		}
		if (this.grammar.isAtom(name)) {
			Content atom = this.atom(name, raw);
			if (atom == null) {
				this.pos = start;
				return false;
			}
			out.add(RichCell.atom(atom, style));
			return true;
		}
		Style inner = this.optionStyle(name, raw, style);
		if (inner == null || this.pos >= this.input.length() || this.input.charAt(this.pos) != '(') {
			this.pos = start;
			return false;
		}
		this.pos++;
		Deque<String> saved = this.open;
		this.open = new ArrayDeque<>();
		List<RichCell> contents = this.sequence(inner, true);
		this.open = saved;
		if (this.pos >= this.input.length() || this.input.charAt(this.pos) != ')') {
			this.pos = start;
			return false;
		}
		this.pos++;
		TextMarkup markup = this.grammar.markup(name);
		out.addAll(markup == null ? contents : this.applyMarkup(markup, this.unescape(raw), contents));
		return true;
	}

	private String value() {
		int start = this.pos;
		while (this.pos < this.input.length()) {
			char c = this.input.charAt(this.pos);
			if (c == '\\' && this.pos + 1 < this.input.length()) {
				this.pos += 2;
				continue;
			}
			if (c == '}') {
				String raw = this.input.substring(start, this.pos);
				this.pos++;
				return raw;
			}
			this.pos++;
		}
		return null;
	}

	private String unescape(String raw) {
		StringBuilder out = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '\\' && i + 1 < raw.length() && this.grammar.escapable(raw.charAt(i + 1))) {
				out.append(raw.charAt(++i));
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private List<String> split(String raw) {
		List<String> parts = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '\\') {
				i++;
			} else if (c == '|') {
				parts.add(this.unescape(raw.substring(start, i)));
				start = i + 1;
			}
		}
		parts.add(this.unescape(raw.substring(start)));
		return parts;
	}

	private Text nested(String value) {
		return new FancyParser(this.grammar, value).parse();
	}

	private Style optionStyle(String name, String raw, Style style) {
		ClickAction action = ClickAction.byName(name);
		if (action != null) {
			return style.withClickEvent(new ClickEvent(action, this.unescape(raw)));
		}
		switch (name) {
			case "show_text":
				return style.withHoverEvent(HoverEvent.showText(this.nested(this.unescape(raw))));
			case "show_item":
				HoverEvent item = this.showItem(this.unescape(raw).trim());
				return item == null ? null : style.withHoverEvent(item);
			case "show_entity":
				HoverEvent entity = this.showEntity(raw.trim());
				return entity == null ? null : style.withHoverEvent(entity);
			case "insertion":
				return style.withInsertion(this.unescape(raw));
			case "font":
				String font = this.unescape(raw).trim();
				return style.withFont(font.isEmpty() ? null : font);
			default:
				return this.grammar.markup(name) == null ? null : style;
		}
	}

	private HoverEvent showItem(String value) {
		int space = value.indexOf(' ');
		String id = space < 0 ? value : value.substring(0, space);
		String rest = space < 0 ? "" : value.substring(space + 1).trim();
		if (id.isEmpty()) {
			return null;
		}
		int count = 1;
		if (!rest.isEmpty() && !rest.startsWith("{")) {
			int next = rest.indexOf(' ');
			try {
				count = Integer.parseInt(next < 0 ? rest : rest.substring(0, next));
			} catch (NumberFormatException e) {
				return null;
			}
			rest = next < 0 ? "" : rest.substring(next + 1).trim();
		}
		CompoundTag data = null;
		if (!rest.isEmpty()) {
			try {
				data = SnbtUtil.parseCompound(rest, SnbtDialect.MODERN);
			} catch (SnbtException e) {
				return null;
			}
		}
		return HoverEvent.showItem(id, count, data);
	}

	private HoverEvent showEntity(String raw) {
		String[] parts = raw.split(" ", 3);
		if (parts.length < 2) {
			return null;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(this.unescape(parts[1]));
		} catch (IllegalArgumentException e) {
			return null;
		}
		Text name = parts.length > 2 ? this.nested(this.unescape(parts[2])) : null;
		return HoverEvent.showEntity(this.unescape(parts[0]), uuid, name);
	}

	private Content atom(String name, String raw) {
		List<String> parts = this.split(raw);
		switch (name) {
			case "translate":
				if (parts.get(0).isEmpty()) {
					return null;
				}
				List<Object> args = new ArrayList<>();
				for (int i = 1; i < parts.size(); i++) {
					args.add(this.nested(parts.get(i)));
				}
				return new TranslatableContent(parts.get(0), null, args);
			case "keybind":
				return raw.isEmpty() ? null : new KeybindContent(this.unescape(raw));
			case "score":
				return parts.size() == 2 && !parts.get(0).isEmpty() ? new ScoreContent(parts.get(0), parts.get(1)) : null;
			case "selector":
				return raw.isEmpty() ? null : new SelectorContent(this.unescape(raw), null);
			case "nbt":
				if (parts.size() < 3 || !List.of("block", "entity", "storage").contains(parts.get(0))) {
					return null;
				}
				boolean interpret = parts.size() > 3 && "interpret".equals(parts.get(3));
				return new NbtContent(parts.get(2), interpret, null, parts.get(0), parts.get(1));
			default:
				return null;
		}
	}
}
