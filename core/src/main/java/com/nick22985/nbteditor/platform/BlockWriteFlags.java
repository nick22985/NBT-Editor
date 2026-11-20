package com.nick22985.nbteditor.platform;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BlockWriteFlags(boolean updateNeighbors, boolean notifyClients, boolean keepState, boolean triggerUpdates, boolean recreate) {
	public static final BlockWriteFlags DEFAULT = new BlockWriteFlags(true, true, false, true, false);
	public static final BlockWriteFlags SILENT = new BlockWriteFlags(false, true, false, false, false);
}
