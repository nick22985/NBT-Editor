package com.nick22985.nbteditor.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GameVersion implements Comparable<GameVersion> {
	private static final Map<String, GameVersion> KNOWN = new LinkedHashMap<>();

	public static final GameVersion V1_12_2 = known("1.12.2", 1343);
	public static final GameVersion V1_13 = known("1.13", 1519);
	public static final GameVersion V1_14 = known("1.14", 1952);
	public static final GameVersion V1_15 = known("1.15", 2225);
	public static final GameVersion V1_16 = known("1.16", 2566);
	public static final GameVersion V1_16_5 = known("1.16.5", 2586);
	public static final GameVersion V1_17 = known("1.17", 2724);
	public static final GameVersion V1_17_1 = known("1.17.1", 2730);
	public static final GameVersion V1_18 = known("1.18", 2860);
	public static final GameVersion V1_18_1 = known("1.18.1", 2865);
	public static final GameVersion V1_18_2 = known("1.18.2", 2975);
	public static final GameVersion V1_19 = known("1.19", 3105);
	public static final GameVersion V1_19_1 = known("1.19.1", 3117);
	public static final GameVersion V1_19_2 = known("1.19.2", 3120);
	public static final GameVersion V1_19_3 = known("1.19.3", 3218);
	public static final GameVersion V1_19_4 = known("1.19.4", 3337);
	public static final GameVersion V1_20 = known("1.20", 3463);
	public static final GameVersion V1_20_1 = known("1.20.1", 3465);
	public static final GameVersion V1_20_2 = known("1.20.2", 3578);
	public static final GameVersion V1_20_3 = known("1.20.3", 3698);
	public static final GameVersion V1_20_4 = known("1.20.4", 3700);
	public static final GameVersion V1_20_5 = known("1.20.5", 3837);
	public static final GameVersion V1_20_6 = known("1.20.6", 3839);
	public static final GameVersion V1_21 = known("1.21", 3953);
	public static final GameVersion V1_21_1 = known("1.21.1", 3955);
	public static final GameVersion V1_21_2 = known("1.21.2", 4080);
	public static final GameVersion V1_21_3 = known("1.21.3", 4082);
	public static final GameVersion V1_21_4 = known("1.21.4", 4189);
	public static final GameVersion V1_21_5 = known("1.21.5", 4325);
	public static final GameVersion V1_21_6 = known("1.21.6", 4435);
	public static final GameVersion V1_21_7 = known("1.21.7", 4438);
	public static final GameVersion V1_21_8 = known("1.21.8", 4440);
	public static final GameVersion V1_21_9 = known("1.21.9", 4554);
	public static final GameVersion V1_21_10 = known("1.21.10", 4556);
	public static final GameVersion V1_21_11 = known("1.21.11", 4671);
	public static final GameVersion V26_1 = known("26.1", 4786);
	public static final GameVersion V26_1_2 = known("26.1.2", 4790);
	public static final GameVersion V26_2 = known("26.2", 4903);
	public static final GameVersion V26_3 = known("26.3", 5023);

	private final String name;
	private final int dataVersion;

	private GameVersion(String name, int dataVersion) {
		this.name = name;
		this.dataVersion = dataVersion;
	}

	private static GameVersion known(String name, int dataVersion) {
		GameVersion version = new GameVersion(name, dataVersion);
		KNOWN.put(name, version);
		return version;
	}

	public static GameVersion of(String name, int dataVersion) {
		GameVersion known = KNOWN.get(name);
		if (known != null && known.dataVersion == dataVersion) {
			return known;
		}
		return new GameVersion(name, dataVersion);
	}

	public static GameVersion byName(String name) {
		return KNOWN.get(name);
	}

	public static GameVersion byDataVersion(int dataVersion) {
		GameVersion best = null;
		for (GameVersion version : KNOWN.values()) {
			if (version.dataVersion == dataVersion) {
				return version;
			}
			if (version.dataVersion < dataVersion && (best == null || version.dataVersion > best.dataVersion)) {
				best = version;
			}
		}
		String name = best == null ? "unknown" : best.name + "+";
		return new GameVersion(name + " (" + dataVersion + ")", dataVersion);
	}

	public static List<GameVersion> known() {
		List<GameVersion> list = new ArrayList<>(KNOWN.values());
		Collections.sort(list);
		return Collections.unmodifiableList(list);
	}

	public String name() {
		return name;
	}

	public int dataVersion() {
		return dataVersion;
	}

	public boolean atLeast(GameVersion other) {
		return dataVersion >= other.dataVersion;
	}

	public boolean below(GameVersion other) {
		return dataVersion < other.dataVersion;
	}

	public boolean has(Feature feature) {
		return feature.in(this);
	}

	@Override
	public int compareTo(GameVersion other) {
		return Integer.compare(dataVersion, other.dataVersion);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof GameVersion && ((GameVersion) o).dataVersion == dataVersion;
	}

	@Override
	public int hashCode() {
		return dataVersion;
	}

	@Override
	public String toString() {
		return name;
	}
}
