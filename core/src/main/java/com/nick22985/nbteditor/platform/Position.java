package com.nick22985.nbteditor.platform;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record Position(double x, double y, double z) {
	public static final Position ORIGIN = new Position(0, 0, 0);

	public BlockPos block() {
		return new BlockPos((int) Math.floor(this.x), (int) Math.floor(this.y), (int) Math.floor(this.z));
	}
}
