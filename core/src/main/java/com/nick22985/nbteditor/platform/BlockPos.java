package com.nick22985.nbteditor.platform;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BlockPos(int x, int y, int z) {
	public static BlockPos of(int x, int y, int z) {
		return new BlockPos(x, y, z);
	}

	public BlockPos offset(int dx, int dy, int dz) {
		return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
	}

	public String command() {
		return this.x + " " + this.y + " " + this.z;
	}
}
