package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.NbtSuggestPrefix;
import com.nick22985.nbteditor.editor.NbtSuggestSubject;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.SnbtWriter;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.ui.suggest.TextSuggestion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtSuggestUtil {
	private static final SnbtWriter WRITER = SnbtWriter.compact();

	private NbtSuggestUtil() {
	}

	public static NbtSuggestSubject subject(EditorSession session) {
		CompoundTag root = session.root();
		SubjectKind kind = session.kind();
		if (kind == SubjectKind.ITEM) {
			String id = ItemStackUtil.id(root);
			if (id == null || id.isEmpty()) {
				return null;
			}
			return new NbtSuggestSubject("item", ItemStackUtil.namespaced(id), session.version().has(Feature.ITEM_COMPONENTS), session.dataPath());
		}
		if (kind == SubjectKind.BLOCK) {
			String id = root.getString(BlockDocUtil.NAME, "");
			return id.isEmpty() ? null : new NbtSuggestSubject("block", ItemStackUtil.namespaced(id), false, session.dataPath());
		}
		if (kind == SubjectKind.PLAYER) {
			return new NbtSuggestSubject("entity", "minecraft:player", false, session.dataPath());
		}
		if (kind == SubjectKind.ENTITY) {
			String id = root.getString("id", "");
			return id.isEmpty() ? null : new NbtSuggestSubject("entity", ItemStackUtil.namespaced(id), false, session.dataPath());
		}
		return null;
	}

	public static String escapeKey(String key) {
		return WRITER.key(key);
	}

	public static String openKey(String key) {
		String escaped = escapeKey(key);
		return escaped.equals(key) ? key : escaped.substring(0, escaped.length() - 1);
	}

	public static NbtSuggestPrefix prefix(Tag start, List<Object> path, String key, String value) {
		StringBuilder out = new StringBuilder();
		Tag tag = start;
		for (Object segment : path) {
			if (tag instanceof CompoundTag && segment instanceof String) {
				out.append('{').append(escapeKey((String) segment)).append(':');
			} else if (tag instanceof ListTag && segment instanceof Integer) {
				out.append('[');
			} else {
				return null;
			}
			tag = NbtPathUtil.step(tag, segment);
		}
		if (tag instanceof ListTag) {
			if (value == null) {
				return null;
			}
			out.append('[');
			int fieldStart = out.length();
			out.append(value);
			return new NbtSuggestPrefix(out.toString(), fieldStart);
		}
		if (tag != null && !(tag instanceof CompoundTag)) {
			return null;
		}
		out.append('{');
		if (value == null) {
			int fieldStart = out.length();
			out.append(openKey(key));
			return new NbtSuggestPrefix(out.toString(), fieldStart);
		}
		out.append(escapeKey(key)).append(':');
		int fieldStart = out.length();
		out.append(value);
		return new NbtSuggestPrefix(out.toString(), fieldStart);
	}

	public static String openString(String value) {
		String quoted = WRITER.quote(value);
		return quoted.substring(0, quoted.length() - 1);
	}

	public static List<TextSuggestion> unquoteAll(List<TextSuggestion> suggestions, int typed) {
		List<TextSuggestion> out = new ArrayList<>();
		for (TextSuggestion suggestion : suggestions) {
			String text = suggestion.text();
			if (suggestion.start() != 0 || text.isEmpty() || text.charAt(0) != '"' && text.charAt(0) != '\'') {
				continue;
			}
			String closed = text.length() > 1 && text.charAt(text.length() - 1) == text.charAt(0) ? text : text + text.charAt(0);
			out.add(new TextSuggestion(0, typed, unquote(closed), suggestion.tooltip()));
		}
		return out;
	}

	public static int depth(String value) {
		int depth = 0;
		char quote = 0;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (quote != 0) {
				if (c == '\\') {
					i++;
				} else if (c == quote) {
					quote = 0;
				}
			} else if (c == '"' || c == '\'') {
				quote = c;
			} else if (c == '{' || c == '[') {
				depth++;
			} else if ((c == '}' || c == ']') && depth > 0) {
				depth--;
			}
		}
		return depth;
	}

	public static boolean closesOutside(String suggestion, String value) {
		if (suggestion.isEmpty()) {
			return false;
		}
		char first = suggestion.charAt(0);
		return (first == ',' || first == '}' || first == ']') && depth(value) == 0;
	}

	public static String unquote(String text) {
		if (text.length() < 2) {
			return text;
		}
		char quote = text.charAt(0);
		if (quote != '"' && quote != '\'' || text.charAt(text.length() - 1) != quote) {
			return text;
		}
		StringBuilder out = new StringBuilder();
		for (int i = 1; i < text.length() - 1; i++) {
			char c = text.charAt(i);
			if (c == '\\' && i + 1 < text.length() - 1) {
				i++;
				c = text.charAt(i);
			}
			out.append(c);
		}
		return out.toString();
	}

	public static String componentId(String key) {
		String id = key.startsWith("!") ? key.substring(1) : key;
		return ItemStackUtil.namespaced(id);
	}

	public static boolean componentTaken(Collection<String> siblings, String id) {
		for (String sibling : siblings) {
			if (componentId(sibling).equals(id)) {
				return true;
			}
		}
		return false;
	}

	public static boolean componentMatches(String typed, String id) {
		String bare = typed.startsWith("!") ? typed.substring(1) : typed;
		if (bare.isEmpty()) {
			return true;
		}
		if (bare.startsWith(":")) {
			return id.startsWith("minecraft" + bare);
		}
		if (bare.indexOf(':') >= 0) {
			return id.startsWith(bare);
		}
		return id.startsWith(bare) || ItemStackUtil.stripNamespace(id).startsWith(bare);
	}
}
