package com.nick22985.nbteditor.text;

import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Style {
	public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null, null);

	private final TextColor color;
	private final Integer shadowColor;
	private final Boolean bold;
	private final Boolean italic;
	private final Boolean underlined;
	private final Boolean strikethrough;
	private final Boolean obfuscated;
	private final String font;
	private final String insertion;
	private final ClickEvent clickEvent;
	private final HoverEvent hoverEvent;

	private Style(TextColor color, Integer shadowColor, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough, Boolean obfuscated, String font, String insertion, ClickEvent clickEvent, HoverEvent hoverEvent) {
		this.color = color;
		this.shadowColor = shadowColor;
		this.bold = bold;
		this.italic = italic;
		this.underlined = underlined;
		this.strikethrough = strikethrough;
		this.obfuscated = obfuscated;
		this.font = font;
		this.insertion = insertion;
		this.clickEvent = clickEvent;
		this.hoverEvent = hoverEvent;
	}

	public TextColor color() {
		return color;
	}

	public Integer shadowColor() {
		return shadowColor;
	}

	public Boolean bold() {
		return bold;
	}

	public Boolean italic() {
		return italic;
	}

	public Boolean underlined() {
		return underlined;
	}

	public Boolean strikethrough() {
		return strikethrough;
	}

	public Boolean obfuscated() {
		return obfuscated;
	}

	public String font() {
		return font;
	}

	public String insertion() {
		return insertion;
	}

	public ClickEvent clickEvent() {
		return clickEvent;
	}

	public HoverEvent hoverEvent() {
		return hoverEvent;
	}

	public boolean isBold() {
		return Boolean.TRUE.equals(bold);
	}

	public boolean isItalic() {
		return Boolean.TRUE.equals(italic);
	}

	public boolean isUnderlined() {
		return Boolean.TRUE.equals(underlined);
	}

	public boolean isStrikethrough() {
		return Boolean.TRUE.equals(strikethrough);
	}

	public boolean isObfuscated() {
		return Boolean.TRUE.equals(obfuscated);
	}

	public boolean isEmpty() {
		return equals(EMPTY);
	}

	public Style withColor(TextColor color) {
		return new Style(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, font, insertion, clickEvent, hoverEvent);
	}

	public Style withColor(Formatting formatting) {
		return withColor(formatting == null ? null : TextColor.of(formatting));
	}

	public Style withColor(int rgb) {
		return withColor(TextColor.rgb(rgb));
	}

	public Style withShadowColor(Integer argb) {
		return new Style(color, argb, bold, italic, underlined, strikethrough, obfuscated, font, insertion, clickEvent, hoverEvent);
	}

	public Style withBold(Boolean value) {
		return new Style(color, shadowColor, value, italic, underlined, strikethrough, obfuscated, font, insertion, clickEvent, hoverEvent);
	}

	public Style withItalic(Boolean value) {
		return new Style(color, shadowColor, bold, value, underlined, strikethrough, obfuscated, font, insertion, clickEvent, hoverEvent);
	}

	public Style withUnderlined(Boolean value) {
		return new Style(color, shadowColor, bold, italic, value, strikethrough, obfuscated, font, insertion, clickEvent, hoverEvent);
	}

	public Style withStrikethrough(Boolean value) {
		return new Style(color, shadowColor, bold, italic, underlined, value, obfuscated, font, insertion, clickEvent, hoverEvent);
	}

	public Style withObfuscated(Boolean value) {
		return new Style(color, shadowColor, bold, italic, underlined, strikethrough, value, font, insertion, clickEvent, hoverEvent);
	}

	public Style withFont(String value) {
		return new Style(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, value, insertion, clickEvent, hoverEvent);
	}

	public Style withInsertion(String value) {
		return new Style(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, font, value, clickEvent, hoverEvent);
	}

	public Style withClickEvent(ClickEvent value) {
		return new Style(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, font, insertion, value, hoverEvent);
	}

	public Style withHoverEvent(HoverEvent value) {
		return new Style(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, font, insertion, clickEvent, value);
	}

	public Style apply(Formatting formatting) {
		switch (formatting) {
			case BOLD:
				return withBold(true);
			case ITALIC:
				return withItalic(true);
			case UNDERLINE:
				return withUnderlined(true);
			case STRIKETHROUGH:
				return withStrikethrough(true);
			case OBFUSCATED:
				return withObfuscated(true);
			case RESET:
				return EMPTY;
			default:
				return new Style(TextColor.of(formatting), shadowColor, null, null, null, null, null, font, insertion, clickEvent, hoverEvent);
		}
	}

	public Style inheritFrom(Style parent) {
		if (parent == null || parent.isEmpty()) {
			return this;
		}
		return new Style(
			color != null ? color : parent.color,
			shadowColor != null ? shadowColor : parent.shadowColor,
			bold != null ? bold : parent.bold,
			italic != null ? italic : parent.italic,
			underlined != null ? underlined : parent.underlined,
			strikethrough != null ? strikethrough : parent.strikethrough,
			obfuscated != null ? obfuscated : parent.obfuscated,
			font != null ? font : parent.font,
			insertion != null ? insertion : parent.insertion,
			clickEvent != null ? clickEvent : parent.clickEvent,
			hoverEvent != null ? hoverEvent : parent.hoverEvent);
	}

	public Style common(Style other) {
		return new Style(
			Objects.equals(color, other.color) ? color : null,
			Objects.equals(shadowColor, other.shadowColor) ? shadowColor : null,
			Objects.equals(bold, other.bold) ? bold : null,
			Objects.equals(italic, other.italic) ? italic : null,
			Objects.equals(underlined, other.underlined) ? underlined : null,
			Objects.equals(strikethrough, other.strikethrough) ? strikethrough : null,
			Objects.equals(obfuscated, other.obfuscated) ? obfuscated : null,
			Objects.equals(font, other.font) ? font : null,
			Objects.equals(insertion, other.insertion) ? insertion : null,
			Objects.equals(clickEvent, other.clickEvent) ? clickEvent : null,
			Objects.equals(hoverEvent, other.hoverEvent) ? hoverEvent : null);
	}

	public Style without(Style inherited) {
		return new Style(
			Objects.equals(color, inherited.color) ? null : color,
			Objects.equals(shadowColor, inherited.shadowColor) ? null : shadowColor,
			Objects.equals(bold, inherited.bold) ? null : bold,
			Objects.equals(italic, inherited.italic) ? null : italic,
			Objects.equals(underlined, inherited.underlined) ? null : underlined,
			Objects.equals(strikethrough, inherited.strikethrough) ? null : strikethrough,
			Objects.equals(obfuscated, inherited.obfuscated) ? null : obfuscated,
			Objects.equals(font, inherited.font) ? null : font,
			Objects.equals(insertion, inherited.insertion) ? null : insertion,
			Objects.equals(clickEvent, inherited.clickEvent) ? null : clickEvent,
			Objects.equals(hoverEvent, inherited.hoverEvent) ? null : hoverEvent);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Style)) {
			return false;
		}
		Style other = (Style) o;
		return Objects.equals(color, other.color) && Objects.equals(shadowColor, other.shadowColor) && Objects.equals(bold, other.bold) && Objects.equals(italic, other.italic) && Objects.equals(underlined, other.underlined) && Objects.equals(strikethrough, other.strikethrough) && Objects.equals(obfuscated, other.obfuscated) && Objects.equals(font, other.font) && Objects.equals(insertion, other.insertion) && Objects.equals(clickEvent, other.clickEvent) && Objects.equals(hoverEvent, other.hoverEvent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, font, insertion, clickEvent, hoverEvent);
	}
}
