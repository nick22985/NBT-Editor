package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.data.GameVersion;
import net.fabricmc.loader.api.FabricLoader;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McVersionUtil {
	private static GameVersion current;

	private McVersionUtil() {
	}

	public static String name() {
		return FabricLoader.getInstance().getModContainer("minecraft").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}

	public static GameVersion current() {
		if (current == null) {
			current = closest(name());
		}
		return current;
	}

	public static GameVersion closest(String name) {
		GameVersion known = GameVersion.byName(name);
		if (known != null) {
			return known;
		}
		int[] wanted = parts(name);
		GameVersion best = null;
		for (GameVersion version : GameVersion.known()) {
			if (compare(parts(version.name()), wanted) <= 0) {
				best = version;
			}
		}
		return best == null ? GameVersion.V1_17_1 : best;
	}

	private static int[] parts(String name) {
		String[] split = name.split("[^0-9]+");
		int[] out = new int[3];
		int index = 0;
		for (String part : split) {
			if (part.isEmpty() || index >= out.length) {
				continue;
			}
			try {
				out[index++] = Integer.parseInt(part);
			} catch (NumberFormatException e) {
				out[index++] = 0;
			}
		}
		return out;
	}

	private static int compare(int[] a, int[] b) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return Integer.compare(a[i], b[i]);
			}
		}
		return 0;
	}
}
