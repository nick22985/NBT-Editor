package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.module.Registry;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.text.TextMarkup;
import java.util.List;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FancyGrammar {
	public static final String ESCAPABLE = "\\&§()[]{}<|;";
	public static final List<String> OPTIONS = List.of("open_url", "open_file", "run_command", "suggest_command", "change_page", "copy_to_clipboard", "show_text", "show_item", "show_entity", "insertion", "font");
	public static final List<String> ATOMS = List.of("translate", "keybind", "score", "selector", "nbt");
	public static final String COLOR = "color";
	public static final String RESET = "reset";
	public static final String NEWLINE = "newline";

	private final Registry<TextMarkup> markups;

	public FancyGrammar(Registry<TextMarkup> markups) {
		this.markups = markups;
	}

	public boolean isOption(String name) {
		return OPTIONS.contains(name);
	}

	public boolean isAtom(String name) {
		return ATOMS.contains(name);
	}

	public TextMarkup markup(String name) {
		return this.markups == null ? null : this.markups.get(name).orElse(null);
	}

	public boolean known(String name) {
		return this.isOption(name) || this.isAtom(name) || this.markup(name) != null;
	}

	public boolean escapable(char c) {
		return ESCAPABLE.indexOf(c) >= 0;
	}

	public boolean formsCode(char next) {
		return next == '#' || next == 'x' || next == 'X' || next == '_' || Formatting.byCode(next) != null;
	}

	public String optionAt(CharSequence text, int at) {
		if (at >= text.length() || text.charAt(at) != '[') {
			return null;
		}
		int end = at + 1;
		while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) {
			end++;
		}
		if (end + 1 >= text.length() || text.charAt(end) != ']' || text.charAt(end + 1) != '{') {
			return null;
		}
		String name = text.subSequence(at + 1, end).toString();
		return this.known(name) ? name : null;
	}

	public FancyTag tagAt(CharSequence text, int at) {
		if (at >= text.length() || text.charAt(at) != '<') {
			return null;
		}
		int end = at + 1;
		while (end < text.length() && text.charAt(end) != '>' && text.charAt(end) != '<' && text.charAt(end) != '\n') {
			end++;
		}
		if (end >= text.length() || text.charAt(end) != '>') {
			return null;
		}
		return this.tag(text.subSequence(at + 1, end).toString(), end + 1 - at);
	}

	private FancyTag tag(String inner, int length) {
		boolean closing = inner.startsWith("/");
		String body = closing ? inner.substring(1) : inner;
		if (body.isEmpty()) {
			return null;
		}
		int colon = body.indexOf(':');
		String name = (colon < 0 ? body : body.substring(0, colon)).toLowerCase(Locale.ROOT);
		String argument = colon < 0 ? "" : body.substring(colon + 1).replace(':', ' ').trim();
		if (name.startsWith("#") || Formatting.byName(name) != null && Formatting.byName(name).isColor()) {
			return TextColor.parse(name) == null ? null : new FancyTag(COLOR, closing, name, length);
		}
		switch (name) {
			case "color":
			case "colour":
			case "c":
				return closing || TextColor.parse(argument) != null ? new FancyTag(COLOR, closing, argument, length) : null;
			case "b":
			case "bold":
				return new FancyTag("bold", closing, "", length);
			case "i":
			case "em":
			case "italic":
				return new FancyTag("italic", closing, "", length);
			case "u":
			case "underline":
			case "underlined":
				return new FancyTag("underlined", closing, "", length);
			case "st":
			case "strikethrough":
				return new FancyTag("strikethrough", closing, "", length);
			case "obf":
			case "obfuscated":
				return new FancyTag("obfuscated", closing, "", length);
			case "reset":
				return closing ? null : new FancyTag(RESET, false, "", length);
			case "br":
			case "newline":
				return closing ? null : new FancyTag(NEWLINE, false, "", length);
			default:
				return this.markup(name) == null ? null : new FancyTag(name, closing, argument, length);
		}
	}
}
