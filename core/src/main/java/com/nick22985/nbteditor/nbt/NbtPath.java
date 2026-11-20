package com.nick22985.nbteditor.nbt;

import com.nick22985.nbteditor.util.NbtPathUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtPath implements Comparable<NbtPath> {
	public static final NbtPath ROOT = new NbtPath(List.of());

	private final List<Object> segments;

	private NbtPath(List<Object> segments) {
		this.segments = segments;
	}

	public static NbtPath of(Object... segments) {
		List<Object> list = new ArrayList<>();
		for (Object segment : segments) {
			if (!(segment instanceof String) && !(segment instanceof Integer)) {
				throw new IllegalArgumentException("Path segments must be strings or integers");
			}
			list.add(segment);
		}
		return new NbtPath(Collections.unmodifiableList(list));
	}

	public static NbtPath parse(String text) {
		List<Object> list = new ArrayList<>();
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '.') {
				i++;
				continue;
			}
			if (c == '[') {
				int close = text.indexOf(']', i);
				if (close < 0) {
					throw new IllegalArgumentException("Unclosed index in " + text);
				}
				list.add(Integer.parseInt(text.substring(i + 1, close).trim()));
				i = close + 1;
				continue;
			}
			if (c == '"') {
				StringBuilder key = new StringBuilder();
				i++;
				while (i < text.length() && text.charAt(i) != '"') {
					if (text.charAt(i) == '\\' && i + 1 < text.length()) {
						i++;
					}
					key.append(text.charAt(i));
					i++;
				}
				list.add(key.toString());
				i++;
				continue;
			}
			int start = i;
			while (i < text.length() && text.charAt(i) != '.' && text.charAt(i) != '[') {
				i++;
			}
			list.add(text.substring(start, i));
		}
		return new NbtPath(Collections.unmodifiableList(list));
	}

	public List<Object> segments() {
		return segments;
	}

	public int size() {
		return segments.size();
	}

	public boolean isRoot() {
		return segments.isEmpty();
	}

	public Object last() {
		return segments.isEmpty() ? null : segments.get(segments.size() - 1);
	}

	public NbtPath parent() {
		if (segments.isEmpty()) {
			return null;
		}
		return new NbtPath(segments.subList(0, segments.size() - 1));
	}

	public NbtPath child(String key) {
		List<Object> list = new ArrayList<>(segments);
		list.add(key);
		return new NbtPath(Collections.unmodifiableList(list));
	}

	public NbtPath child(int index) {
		List<Object> list = new ArrayList<>(segments);
		list.add(index);
		return new NbtPath(Collections.unmodifiableList(list));
	}

	public NbtPath resolve(NbtPath other) {
		List<Object> list = new ArrayList<>(segments);
		list.addAll(other.segments);
		return new NbtPath(Collections.unmodifiableList(list));
	}

	public NbtPath withLast(Object segment) {
		List<Object> list = new ArrayList<>(segments);
		list.set(list.size() - 1, segment);
		return new NbtPath(Collections.unmodifiableList(list));
	}

	public boolean startsWith(NbtPath prefix) {
		if (prefix.segments.size() > segments.size()) {
			return false;
		}
		for (int i = 0; i < prefix.segments.size(); i++) {
			if (!prefix.segments.get(i).equals(segments.get(i))) {
				return false;
			}
		}
		return true;
	}

	public Tag get(Tag root) {
		Tag current = root;
		for (Object segment : segments) {
			current = NbtPathUtil.step(current, segment);
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		for (Object segment : segments) {
			if (segment instanceof String && out.length() > 0) {
				out.append('.');
			}
			out.append(NbtPathUtil.formatSegment(segment));
		}
		return out.toString();
	}

	@Override
	public int compareTo(NbtPath other) {
		int shared = Math.min(segments.size(), other.segments.size());
		for (int i = 0; i < shared; i++) {
			Object a = segments.get(i);
			Object b = other.segments.get(i);
			if (a instanceof Integer && b instanceof Integer) {
				int cmp = Integer.compare((Integer) a, (Integer) b);
				if (cmp != 0) {
					return cmp;
				}
			} else {
				int cmp = a.toString().compareTo(b.toString());
				if (cmp != 0) {
					return cmp;
				}
			}
		}
		return Integer.compare(segments.size(), other.segments.size());
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof NbtPath && ((NbtPath) o).segments.equals(segments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(segments);
	}
}
