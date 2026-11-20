package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.ClickAction;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClickEventUtil {
	private ClickEventUtil() {
	}

	public static boolean validValue(ClickAction action, String value) {
		if (action == null || value == null || value.isEmpty()) {
			return true;
		}
		if (action == ClickAction.OPEN_URL) {
			return validUrl(value);
		}
		if (action == ClickAction.CHANGE_PAGE) {
			return validPage(value);
		}
		return true;
	}

	public static boolean validUrl(String value) {
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			return scheme != null && (scheme.toLowerCase(Locale.ROOT).equals("http") || scheme.toLowerCase(Locale.ROOT).equals("https")) && uri.getHost() != null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	public static boolean validPage(String value) {
		try {
			return Integer.parseInt(value.trim()) > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
