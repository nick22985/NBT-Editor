package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BlockData(String blockId, Map<String, String> properties, CompoundTag blockEntity) {
	public String stateString() {
		if (this.properties.isEmpty()) {
			return this.blockId;
		}
		StringBuilder out = new StringBuilder(this.blockId).append('[');
		boolean first = true;
		for (Map.Entry<String, String> entry : this.properties.entrySet()) {
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append(entry.getKey()).append('=').append(entry.getValue());
		}
		return out.append(']').toString();
	}
}
