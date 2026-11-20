package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.widget.ItemGrid;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelGrid extends ItemGrid {
	private final IntFunction<List<Text>> tooltips;

	public ModelGrid(int columns, int count, IntFunction<GameItem> items, IntFunction<List<Text>> tooltips) {
		super(columns, count, items);
		this.tooltips = tooltips;
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		int index = this.indexAt(mouseX, mouseY);
		return index >= 0 ? this.tooltips.apply(index) : super.tooltip(mouseX, mouseY);
	}
}
