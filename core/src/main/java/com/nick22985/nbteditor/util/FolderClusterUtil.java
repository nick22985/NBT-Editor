package com.nick22985.nbteditor.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FolderClusterUtil {
	private static final int MIN_GROUP = 3;
	private static final String SEPARATOR = "_";

	private FolderClusterUtil() {
	}

	public static Map<String, String> paths(Collection<String> folders) {
		Map<String, Set<String>> children = new HashMap<>();
		for (String folder : folders) {
			if (folder.isEmpty()) {
				continue;
			}
			String parent = "";
			for (String part : folder.split("/")) {
				children.computeIfAbsent(parent, key -> new TreeSet<>()).add(part);
				parent = parent.isEmpty() ? part : parent + "/" + part;
			}
		}
		Map<String, String> out = new HashMap<>();
		out.put("", "");
		walk("", "", children, out);
		return out;
	}

	private static void walk(String real, String display, Map<String, Set<String>> children, Map<String, String> out) {
		Set<String> names = children.get(real);
		if (names == null) {
			return;
		}
		Map<String, List<String>> segments = segments(names);
		for (String name : names) {
			String childReal = real.isEmpty() ? name : real + "/" + name;
			List<String> parts = new ArrayList<>();
			if (!display.isEmpty()) {
				parts.add(display);
			}
			parts.addAll(segments.get(name));
			String childDisplay = String.join("/", parts);
			out.put(childReal, childDisplay);
			walk(childReal, childDisplay, children, out);
		}
	}

	public static Map<String, List<String>> segments(Collection<String> names) {
		Map<String, List<String>> out = new HashMap<>();
		cluster(new ArrayList<>(names), List.of(), 0, out);
		return out;
	}

	private static void cluster(List<String> names, List<String> parent, int consumed, Map<String, List<String>> out) {
		Map<String, List<String>> buckets = new TreeMap<>();
		for (String name : names) {
			buckets.computeIfAbsent(tokens(name)[consumed], key -> new ArrayList<>()).add(name);
		}
		for (List<String> bucket : buckets.values()) {
			int common = common(bucket);
			boolean anchored = bucket.stream().anyMatch(name -> tokens(name).length == common);
			boolean whole = bucket.size() == names.size() && !anchored;
			String label = label(bucket.get(0), consumed, common);
			if (!whole && !label.isEmpty() && (bucket.size() >= MIN_GROUP || (bucket.size() >= 2 && anchored))) {
				List<String> group = append(parent, label);
				List<String> rest = new ArrayList<>();
				for (String name : bucket) {
					if (tokens(name).length == common) {
						out.put(name, group);
					} else {
						rest.add(name);
					}
				}
				if (!rest.isEmpty()) {
					cluster(rest, group, common, out);
				}
				continue;
			}
			for (String name : bucket) {
				String leaf = label(name, consumed, tokens(name).length);
				out.put(name, append(parent, leaf.isEmpty() ? name : leaf));
			}
		}
	}

	private static String[] tokens(String name) {
		return name.split(SEPARATOR, -1);
	}

	private static int common(List<String> names) {
		String[] first = tokens(names.get(0));
		int length = first.length;
		for (String name : names) {
			String[] other = tokens(name);
			int same = 0;
			while (same < Math.min(length, other.length) && first[same].equals(other[same])) {
				same++;
			}
			length = same;
		}
		return length;
	}

	private static String label(String name, int from, int to) {
		return String.join(SEPARATOR, Arrays.asList(tokens(name)).subList(from, to));
	}

	private static List<String> append(List<String> parent, String segment) {
		List<String> out = new ArrayList<>(parent);
		out.add(segment);
		return out;
	}
}
