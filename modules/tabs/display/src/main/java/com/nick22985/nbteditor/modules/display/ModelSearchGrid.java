package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.ui.widget.TextField;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelSearchGrid extends WidgetGroup {
	private final TextField search;
	private final ModelGrid grid;

	public ModelSearchGrid(int columns, int count, IntFunction<GameItem> items, IntFunction<List<Text>> tooltips) {
		this.search = this.add(new TextField(""));
		this.grid = this.add(new ModelGrid(columns, count, items, tooltips));
	}

	public TextField search() {
		return this.search;
	}

	public ModelGrid grid() {
		return this.grid;
	}

	public int preferredHeight() {
		return Theme.FIELD_HEIGHT + 4 + this.grid.rows() * Theme.SLOT;
	}

	@Override
	protected void onLayout() {
		if (this.grid == null) {
			return;
		}
		this.search.bounds(this.x, this.y, this.width, Theme.FIELD_HEIGHT);
		int gridWidth = this.grid.columns() * Theme.SLOT;
		this.grid.bounds(this.x + (this.width - gridWidth) / 2, this.y + Theme.FIELD_HEIGHT + 4, gridWidth, this.grid.rows() * Theme.SLOT);
	}
}
