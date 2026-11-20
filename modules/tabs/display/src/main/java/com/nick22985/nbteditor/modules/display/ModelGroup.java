package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.ModelEntry;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ModelGroup(String key, List<ModelEntry> entries) {
	public ModelEntry first() {
		return this.entries.get(0);
	}
}
