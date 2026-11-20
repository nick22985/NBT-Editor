package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SearchList<T> extends WidgetGroup {
	public static final int ROW_HEIGHT = 14;

	private final List<T> values;
	private final Function<T, Text> labeler;
	private final Function<T, String> detail;
	private final Consumer<T> onPick;
	private final TextField search;
	private final RowList list;
	private final List<T> shown = new ArrayList<>();

	public SearchList(List<T> values, Function<T, Text> labeler, Function<T, String> detail, Consumer<T> onPick) {
		this.values = List.copyOf(values);
		this.labeler = labeler;
		this.detail = detail;
		this.onPick = onPick;
		this.search = this.add(new TextField(""));
		this.search.placeholder(Text.translatable("gui.recipebook.search_hint"));
		this.search.onChange(query -> this.filter());
		this.search.onSubmit(query -> {
			if (!this.shown.isEmpty()) {
				this.onPick.accept(this.shown.get(0));
			}
		});
		this.list = this.add(new RowList());
		this.list.gap(0);
		this.filter();
	}

	public TextField search() {
		return this.search;
	}

	private boolean matches(T value, String query) {
		if (query.isEmpty()) {
			return true;
		}
		if (this.labeler.apply(value).plain().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		String extra = this.detail == null ? null : this.detail.apply(value);
		return extra != null && extra.toLowerCase(Locale.ROOT).contains(query);
	}

	private void filter() {
		String query = this.search.value().trim().toLowerCase(Locale.ROOT);
		this.shown.clear();
		List<Row> rows = new ArrayList<>();
		for (T value : this.values) {
			if (!this.matches(value, query)) {
				continue;
			}
			this.shown.add(value);
			Text label = this.labeler.apply(value);
			String extra = this.detail == null ? null : this.detail.apply(value);
			Row row = new Row(ROW_HEIGHT);
			row.onClick(input -> this.onPick.accept(value));
			row.decorate((canvas, self) -> {
				int used = DrawUtil.textClipped(canvas, label, self.x() + 4, self.y() + 3, self.width() - 8, Theme.TEXT);
				if (extra != null) {
					DrawUtil.textClipped(canvas, extra, self.x() + used + 10, self.y() + 3, self.width() - used - 14, Theme.FAINT);
				}
			});
			rows.add(row);
		}
		this.list.rows(rows);
		this.list.scroll(0);
	}

	@Override
	protected void onLayout() {
		if (this.list == null) {
			return;
		}
		this.search.bounds(this.x, this.y, this.width, Theme.FIELD_HEIGHT);
		int top = this.y + Theme.FIELD_HEIGHT + 4;
		this.list.bounds(this.x, top, this.width, this.y + this.height - top);
	}
}
