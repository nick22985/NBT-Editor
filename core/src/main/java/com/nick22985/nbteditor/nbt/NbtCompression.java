package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum NbtCompression {
	NONE,
	GZIP,
	ZLIB;

	public static NbtCompression detect(byte[] data) {
		if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
			return NbtCompression.GZIP;
		}
		if (data.length >= 2 && (data[0] & 0xFF) == 0x78 && (((data[0] & 0xFF) << 8) | (data[1] & 0xFF)) % 31 == 0) {
			return NbtCompression.ZLIB;
		}
		return NbtCompression.NONE;
	}
}
