package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.ui.Widget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class RowList extends ScrollPanel {
	public static final int ROW_GAP = 3;

	private final List<Row> rows = new ArrayList<>();
	private int gap = ROW_GAP;

	public RowList gap(int gap) {
		this.gap = gap;
		this.onLayout();
		return this;
	}

	public List<Row> rows() {
		return Collections.unmodifiableList(this.rows);
	}

	public void rows(List<Row> rows) {
		Widget focused = this.screen() == null ? null : this.screen().focusedWidget();
		this.clear();
		this.rows.clear();
		for (Row row : rows) {
			this.rows.add(this.add(row));
		}
		this.onLayout();
		if (focused != null && this.isAncestorOf(focused) && this.screen() != null) {
			this.screen().setFocus(focused);
		}
	}

	public Row addRow(Row row) {
		this.rows.add(this.add(row));
		this.onLayout();
		return row;
	}

	@Override
	public int contentHeight() {
		int height = this.padding() * 2;
		for (Row row : this.rows) {
			if (row.visible()) {
				height += row.height() + this.gap;
			}
		}
		return this.rows.isEmpty() ? height : height - this.gap;
	}

	@Override
	protected void onLayout() {
		int top = this.contentY();
		for (Row row : this.rows) {
			if (!row.visible()) {
				continue;
			}
			row.bounds(this.contentX(), top, this.contentWidth(), row.height());
			top += row.height() + this.gap;
		}
		this.scroll(this.scroll());
	}
}
