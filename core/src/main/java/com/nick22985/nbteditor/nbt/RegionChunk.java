package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record RegionChunk(int index, int timestamp, int compression, byte[] data) {
	public int localX() {
		return this.index & 31;
	}

	public int localZ() {
		return this.index >> 5;
	}
}
