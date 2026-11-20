package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtFileUtil {
	public static final int AUTO = 0;
	public static final int INVALID = -1;
	private static final String[] SIZES = {"B", "KB", "MB", "GB"};

	private NbtFileUtil() {
	}

	public static int rawDataVersion(CompoundTag tag) {
		if (tag.containsNumber(ExportUtil.DATA_VERSION)) {
			return tag.getInt(ExportUtil.DATA_VERSION, 0);
		}
		CompoundTag data = tag.getCompound("Data");
		if (data != null && data.containsNumber(ExportUtil.DATA_VERSION)) {
			return data.getInt(ExportUtil.DATA_VERSION, 0);
		}
		CompoundTag schematic = tag.getCompound("Schematic");
		if (schematic != null && schematic.containsNumber(ExportUtil.DATA_VERSION)) {
			return schematic.getInt(ExportUtil.DATA_VERSION, 0);
		}
		if (tag.containsNumber("MinecraftDataVersion")) {
			return tag.getInt("MinecraftDataVersion", 0);
		}
		return 0;
	}

	public static GameVersion dataVersion(CompoundTag tag) {
		int version = rawDataVersion(tag);
		return version > 0 ? GameVersion.byDataVersion(version) : null;
	}

	public static int parseDataVersion(String input) {
		String trimmed = input == null ? "" : input.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("auto")) {
			return AUTO;
		}
		GameVersion named = GameVersion.byName(trimmed);
		if (named != null) {
			return named.dataVersion();
		}
		try {
			int number = Integer.parseInt(trimmed);
			return number > 0 ? number : INVALID;
		} catch (NumberFormatException e) {
			return INVALID;
		}
	}

	public static String size(long bytes) {
		double value = bytes;
		int unit = 0;
		while (value >= 1024 && unit < SIZES.length - 1) {
			value /= 1024;
			unit++;
		}
		return unit == 0 ? bytes + " " + SIZES[0] : String.format(Locale.ROOT, "%.1f %s", value, SIZES[unit]);
	}

	public static Path resolve(Path base, String input) {
		String trimmed = input == null ? "" : input.trim();
		if (trimmed.isEmpty()) {
			return base;
		}
		if (trimmed.startsWith("~/") || trimmed.equals("~")) {
			return Path.of(System.getProperty("user.home")).resolve(trimmed.substring(Math.min(2, trimmed.length()))).normalize();
		}
		Path path = Path.of(trimmed);
		return (path.isAbsolute() ? path : base.resolve(path)).normalize();
	}

	public static long sizeOf(Path path) {
		try {
			return Files.size(path);
		} catch (IOException e) {
			return -1;
		}
	}
}
