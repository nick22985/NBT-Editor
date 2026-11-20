package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.ModelEntry;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ModelLine(int x, int y, int width, int height, ModelGroup header, List<ModelEntry> entries, boolean compact) {
	public int bottom() {
		return this.y + this.height;
	}

	public boolean contains(double px, double py) {
		return py >= this.y && py < this.y + this.height && px >= this.x && px < this.x + this.width;
	}
}
