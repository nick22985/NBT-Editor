package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.util.TextCodecUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Text {
	public static final Text EMPTY = new Text(LiteralContent.EMPTY, Style.EMPTY, List.of());

	private final Content content;
	private final Style style;
	private final List<Text> siblings;

	private Text(Content content, Style style, List<Text> siblings) {
		this.content = Objects.requireNonNull(content);
		this.style = style == null ? Style.EMPTY : style;
		this.siblings = siblings;
	}

	public static Text of(Content content, Style style, List<Text> siblings) {
		return new Text(content, style, siblings == null || siblings.isEmpty() ? List.of() : Collections.unmodifiableList(new ArrayList<>(siblings)));
	}

	public static Text literal(String text) {
		return text.isEmpty() ? EMPTY : new Text(new LiteralContent(text), Style.EMPTY, List.of());
	}

	public static Text literal(String text, Style style) {
		return new Text(new LiteralContent(text), style, List.of());
	}

	public static Text translatable(String key, Object... args) {
		return new Text(new TranslatableContent(key, null, List.of(args)), Style.EMPTY, List.of());
	}

	public static Text keybind(String key) {
		return new Text(new KeybindContent(key), Style.EMPTY, List.of());
	}

	public static Text join(List<Text> parts) {
		if (parts.isEmpty()) {
			return EMPTY;
		}
		if (parts.size() == 1) {
			return parts.get(0);
		}
		return of(LiteralContent.EMPTY, Style.EMPTY, parts);
	}

	public Content content() {
		return content;
	}

	public Style style() {
		return style;
	}

	public List<Text> siblings() {
		return siblings;
	}

	public Text withStyle(Style style) {
		return new Text(content, style, siblings);
	}

	public Text withContent(Content content) {
		return new Text(content, style, siblings);
	}

	public Text withSiblings(List<Text> siblings) {
		return of(content, style, siblings);
	}

	public Text append(Text sibling) {
		List<Text> list = new ArrayList<>(siblings.size() + 1);
		list.addAll(siblings);
		list.add(sibling);
		return new Text(content, style, Collections.unmodifiableList(list));
	}

	public Text append(String literal) {
		return append(literal(literal));
	}

	public boolean isEmpty() {
		return content instanceof LiteralContent && ((LiteralContent) content).text().isEmpty() && siblings.stream().allMatch(Text::isEmpty);
	}

	public String plain() {
		StringBuilder out = new StringBuilder();
		appendPlain(out);
		return out.toString();
	}

	private void appendPlain(StringBuilder out) {
		out.append(content.plain());
		for (Text sibling : siblings) {
			sibling.appendPlain(out);
		}
	}

	public List<TextSpan> flatten() {
		List<TextSpan> out = new ArrayList<>();
		flatten(Style.EMPTY, out);
		return out;
	}

	private void flatten(Style parent, List<TextSpan> out) {
		Style resolved = style.inheritFrom(parent);
		String value = content.plain();
		if (!value.isEmpty()) {
			if (!out.isEmpty() && out.get(out.size() - 1).style().equals(resolved)) {
				TextSpan last = out.remove(out.size() - 1);
				out.add(new TextSpan(last.text() + value, resolved));
			} else {
				out.add(new TextSpan(value, resolved));
			}
		}
		for (Text sibling : siblings) {
			sibling.flatten(resolved, out);
		}
	}

	public static Text fromSpans(List<TextSpan> spans) {
		List<Text> parts = new ArrayList<>(spans.size());
		for (TextSpan span : spans) {
			parts.add(literal(span.text(), span.style()));
		}
		if (parts.size() == 1) {
			return parts.get(0);
		}
		return of(LiteralContent.EMPTY, Style.EMPTY, parts);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Text && ((Text) o).content.equals(content) && ((Text) o).style.equals(style) && ((Text) o).siblings.equals(siblings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(content, style, siblings);
	}

	@Override
	public String toString() {
		return TextCodecUtil.toJson(this, TextDialect.LATEST);
	}
}
