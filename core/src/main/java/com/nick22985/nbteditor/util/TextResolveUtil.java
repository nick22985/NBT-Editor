package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TranslatableContent;
import com.nick22985.nbteditor.ui.UiHost;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextResolveUtil {
	private TextResolveUtil() {
	}

	public static String resolve(Text text, UiHost host) {
		StringBuilder out = new StringBuilder();
		append(out, text, host);
		return out.toString();
	}

	private static void append(StringBuilder out, Text text, UiHost host) {
		if (text.content() instanceof TranslatableContent) {
			out.append(translate((TranslatableContent) text.content(), host));
		} else {
			out.append(text.content().plain());
		}
		for (Text sibling : text.siblings()) {
			append(out, sibling, host);
		}
	}

	private static String translate(TranslatableContent content, UiHost host) {
		if (!host.hasTranslation(content.key())) {
			return content.plain();
		}
		List<Object> args = content.args();
		Object[] resolved = new Object[args.size()];
		for (int i = 0; i < args.size(); i++) {
			Object arg = args.get(i);
			resolved[i] = arg instanceof Text ? resolve((Text) arg, host) : arg;
		}
		return host.translate(content.key(), resolved);
	}
}
