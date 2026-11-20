package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.text.TranslatableContent;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextQueryUtil {
	private TextQueryUtil() {
	}

	public static TranslatableContent firstTranslatable(Text text) {
		if (text.content() instanceof TranslatableContent) {
			return (TranslatableContent) text.content();
		}
		for (Text sibling : text.siblings()) {
			TranslatableContent found = firstTranslatable(sibling);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	public static boolean containsKey(Text text, String key) {
		if (text.content() instanceof TranslatableContent) {
			TranslatableContent content = (TranslatableContent) text.content();
			if (content.key().equals(key)) {
				return true;
			}
			for (Object arg : content.args()) {
				if (arg instanceof Text && containsKey((Text) arg, key)) {
					return true;
				}
			}
		}
		for (Text sibling : text.siblings()) {
			if (containsKey(sibling, key)) {
				return true;
			}
		}
		return false;
	}

	public static String argText(TranslatableContent content, int index) {
		List<Object> args = content.args();
		int resolved = index < 0 ? args.size() + index : index;
		if (resolved < 0 || resolved >= args.size()) {
			return null;
		}
		Object arg = args.get(resolved);
		return arg instanceof Text ? ((Text) arg).plain() : String.valueOf(arg);
	}

	public static boolean isRed(Text text) {
		TextColor red = TextColor.of(Formatting.RED);
		if (red.equals(text.style().color())) {
			return true;
		}
		return !text.siblings().isEmpty() && red.equals(text.siblings().get(0).style().color());
	}
}
