package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.doc.DocumentEvent;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ButtonStyle;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.RowList;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.WidgetSizeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class FormPane implements EditorPane {
	public static final int ROW_HEIGHT = 18;
	public static final int HEADER_HEIGHT = 14;

	protected final EditorSession session;
	protected EditorFrame frame;
	protected RowList list;
	private boolean writing;
	private int headerCursor;
	private final List<Button> header = new ArrayList<>();
	private final List<Button> shrinkable = new ArrayList<>();

	protected FormPane(EditorSession session) {
		this.session = session;
	}

	public EditorSession session() {
		return this.session;
	}

	public EditorFrame frame() {
		return this.frame;
	}

	@Override
	public void build(EditorFrame frame) {
		this.frame = frame;
		this.headerCursor = 0;
		this.header.clear();
		this.shrinkable.clear();
		this.list = frame.add(new RowList());
		int top = frame.bodyY() + this.headerHeight();
		this.list.bounds(frame.bodyX(), top, frame.mainWidth(), frame.bodyY() + frame.bodyHeight() - top - this.footerHeight());
		this.buildExtras(frame);
		this.fitHeader();
		this.populate();
	}

	protected int headerHeight() {
		return 0;
	}

	protected int footerHeight() {
		return 0;
	}

	protected void buildExtras(EditorFrame frame) {
	}

	protected int rowWidth() {
		return this.list.contentWidth();
	}

	protected abstract void rows(List<Row> rows);

	public void populate() {
		if (this.list == null) {
			return;
		}
		int scroll = this.list.scroll();
		List<Row> rows = new ArrayList<>();
		this.rows(rows);
		this.list.rows(rows);
		this.list.scroll(scroll);
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		if (!this.writing) {
			this.refresh();
		}
	}

	protected void refresh() {
		this.populate();
	}

	protected boolean writing() {
		return this.writing;
	}

	protected void edit(String label, Consumer<CompoundTag> mutator) {
		this.edit(label, null, mutator);
	}

	protected void edit(String label, String coalesceKey, Consumer<CompoundTag> mutator) {
		if (!this.session.writable()) {
			return;
		}
		this.writing = true;
		try {
			this.session.edit(label, coalesceKey, mutator);
		} finally {
			this.writing = false;
		}
	}

	protected void editAndRefresh(String label, Consumer<CompoundTag> mutator) {
		this.edit(label, mutator);
		this.populate();
	}

	protected Button headerButton(Text label, Runnable onPress) {
		int width = WidgetSizeUtil.buttonWidth(this.frame.screen().host(), label);
		Button button = this.frame.add(new Button(label, onPress));
		button.bounds(this.frame.bodyX() + this.headerCursor, this.frame.bodyY(), width, Theme.BUTTON_HEIGHT);
		this.headerCursor += width + Theme.GAP;
		this.header.add(button);
		this.shrinkable.add(button);
		return button;
	}

	protected Button headerIconButton(String[] icon, Text tooltip, Runnable onPress) {
		Button button = this.frame.add(new Button(Text.EMPTY, onPress).icon(icon, Theme.TEXT));
		button.bounds(this.frame.bodyX() + this.headerCursor, this.frame.bodyY(), Theme.BUTTON_HEIGHT, Theme.BUTTON_HEIGHT);
		button.tooltip(tooltip);
		this.headerCursor += Theme.BUTTON_HEIGHT + Theme.GAP;
		this.header.add(button);
		return button;
	}

	private void fitHeader() {
		int excess = this.headerCursor - Theme.GAP - this.frame.mainWidth();
		if (excess <= 0 || this.shrinkable.isEmpty()) {
			return;
		}
		int flexible = 0;
		for (Button button : this.shrinkable) {
			flexible += button.width();
		}
		int x = this.frame.bodyX();
		int remaining = excess;
		for (Button button : this.header) {
			int width = button.width();
			if (this.shrinkable.contains(button)) {
				int cut = Math.min(remaining, (int) Math.ceil((double) excess * width / flexible));
				width = Math.max(Theme.BUTTON_HEIGHT, width - cut);
				remaining -= button.width() - width;
			}
			button.bounds(x, button.y(), width, button.height());
			x += width + Theme.GAP;
		}
		this.headerCursor = x - this.frame.bodyX();
	}

	protected Button iconButton(String[] icon, int color, Text tooltip, Runnable onPress) {
		Button button = new Button(Text.EMPTY, onPress).style(ButtonStyle.FLAT).icon(icon, color);
		button.enabled(this.session.writable());
		if (tooltip != null) {
			button.tooltip(tooltip);
		}
		return button;
	}

	public Row header(Text label) {
		Row row = new Row(HEADER_HEIGHT);
		row.decorate((canvas, self) -> DrawUtil.sectionHeading(canvas, label, self.x() + 2, self.y() + 3, self.width() - 4));
		return row;
	}

	public Row labelled(Text label, int labelWidth) {
		Row row = new Row(ROW_HEIGHT);
		row.decorate((canvas, self) -> DrawUtil.textClipped(canvas, label, self.x() + 4, self.y() + (ROW_HEIGHT - 8) / 2, labelWidth - 8, Theme.TEXT));
		return row;
	}

	public <T extends Widget> Row field(Text label, int labelWidth, T widget) {
		Row row = this.labelled(label, labelWidth);
		row.place(widget, labelWidth, 1, this.rowWidth() - labelWidth - 2, ROW_HEIGHT - 2);
		return row;
	}

	public Row note(Text text, int color) {
		Row row = new Row(Theme.LINE + 2);
		row.decorate((canvas, self) -> DrawUtil.textClipped(canvas, text, self.x() + 4, self.y() + 2, self.width() - 8, color));
		return row;
	}
}
