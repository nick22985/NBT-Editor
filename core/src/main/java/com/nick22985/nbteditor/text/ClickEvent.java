package com.nick22985.nbteditor.text;

import java.net.URI;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClickEvent {
	private final ClickAction action;
	private final String value;
	private final Object payload;

	public ClickEvent(ClickAction action, String value) {
		this(action, value, null);
	}

	public ClickEvent(ClickAction action, String value, Object payload) {
		this.action = Objects.requireNonNull(action);
		this.value = value == null ? "" : value;
		this.payload = payload;
	}

	public ClickAction action() {
		return action;
	}

	public String value() {
		return value;
	}

	public Object payload() {
		return payload;
	}

	public String validate() {
		switch (action) {
			case OPEN_URL:
				try {
					URI uri = new URI(value);
					String scheme = uri.getScheme();
					if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
						return "url_scheme";
					}
					return null;
				} catch (Exception e) {
					return "url";
				}
			case CHANGE_PAGE:
				try {
					return Integer.parseInt(value.trim()) >= 1 ? null : "page";
				} catch (NumberFormatException e) {
					return "page";
				}
			case RUN_COMMAND:
			case SUGGEST_COMMAND:
				return value.indexOf('§') >= 0 ? "command" : null;
			case SHOW_DIALOG:
			case CUSTOM:
				return value.isEmpty() ? "id" : null;
			default:
				return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ClickEvent && ((ClickEvent) o).action == action && ((ClickEvent) o).value.equals(value) && Objects.equals(((ClickEvent) o).payload, payload);
	}

	@Override
	public int hashCode() {
		return Objects.hash(action, value, payload);
	}
}
