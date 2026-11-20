package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record RegionPos(int x, int z) {
	public static RegionPos parse(String fileName) {
		String[] parts = fileName.split("\\.");
		if (parts.length != 4 || !parts[0].equals("r")) {
			return null;
		}
		try {
			return new RegionPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public int chunkX(int index) {
		return this.x * 32 + (index & 31);
	}

	public int chunkZ(int index) {
		return this.z * 32 + (index >> 5);
	}
}
