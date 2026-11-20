package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.PanelScreen;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.RowList;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.TextResolveUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestOverviewScreen extends PanelScreen {
	private static final int ROW_HEIGHT = 20;
	private static final int ICON = 17;
	private static final int MAX_ICONS = 9;
	private static final int LABEL_WIDTH = 150;
	private static final int MIN_LISTED = 100;
	private static final int TOOLTIP_LINES = 12;

	private final ModuleContext ctx;
	private final ClientChest chest;
	private final ClientChestStore store;
	private final ClientChestScreen parent;
	private final Set<Integer> selected = new TreeSet<>();
	private final Map<Integer, PagePreview> previews = new HashMap<>();
	private final Set<Integer> requested = new TreeSet<>();
	private String query = "";
	private RowList list;
	private Button delete;

	public ClientChestOverviewScreen(ModuleContext ctx, ClientChest chest, ClientChestScreen parent) {
		super(ctx.tr("overview.title"));
		this.ctx = ctx;
		this.chest = chest;
		this.store = chest.store();
		this.parent = parent;
	}

	@Override
	protected Text subtitle() {
		return this.ctx.tr("overview.subtitle", this.store.count(), this.selected.size());
	}

	@Override
	protected void buildPanel() {
		int checkWidth = 110;
		TextField search = this.add(new TextField(this.query));
		search.placeholder(this.ctx.tr("overview.search"));
		search.onChange(value -> {
			this.query = value;
			this.populate();
		});
		search.bounds(this.bodyX, this.bodyY, this.bodyWidth - checkWidth - Theme.GAP, Theme.FIELD_HEIGHT);
		this.add(new Checkbox(this.ctx.tr("overview.hide_empty"), this.chest.settings().hideEmpty(), value -> {
			this.chest.settings().hideEmpty(value);
			this.populate();
		})).bounds(this.bodyX + this.bodyWidth - checkWidth, this.bodyY + (Theme.FIELD_HEIGHT - Theme.BUTTON_HEIGHT) / 2, checkWidth, Theme.BUTTON_HEIGHT);
		int listY = this.bodyY + Theme.FIELD_HEIGHT + Theme.GAP;
		this.list = this.add(new RowList());
		this.list.bounds(this.bodyX, listY, this.bodyWidth, this.bodyY + this.bodyHeight - listY);
		this.doneButton();
		this.delete = this.footerButton(this.ctx.tr("overview.delete"), this::confirmDelete);
		this.footerButton(this.ctx.tr("overview.select_all"), this::selectAll);
		this.footerButton(this.ctx.tr("overview.select_none"), () -> {
			this.selected.clear();
			this.populate();
		});
		this.populate();
		this.setFocus(search);
	}

	private List<Integer> pages() {
		List<Integer> pages = new ArrayList<>();
		if (this.chest.settings().hideEmpty()) {
			TreeSet<Integer> interesting = new TreeSet<>(this.store.filled());
			interesting.addAll(this.store.names().keySet());
			for (int page : interesting) {
				if (page < this.chest.pages()) {
					pages.add(page);
				}
			}
		} else {
			int count = Math.min(this.chest.pages(), Math.max(MIN_LISTED, this.store.last() + 2));
			for (int page = 0; page < count; page++) {
				pages.add(page);
			}
		}
		pages.removeIf(page -> !this.matches(page));
		return pages;
	}

	private boolean matches(int page) {
		String wanted = this.query.trim().toLowerCase(Locale.ROOT);
		if (wanted.isEmpty() || String.valueOf(page + 1).startsWith(wanted)) {
			return true;
		}
		String name = this.store.name(page);
		if (name != null && name.toLowerCase(Locale.ROOT).contains(wanted)) {
			return true;
		}
		PagePreview preview = this.preview(page);
		if (preview != null) {
			for (GameItem item : preview.items()) {
				if (TextResolveUtil.resolve(item.name(), this.host()).toLowerCase(Locale.ROOT).contains(wanted) || item.id().toLowerCase(Locale.ROOT).contains(wanted)) {
					return true;
				}
			}
		}
		return false;
	}

	private PagePreview preview(int page) {
		ClientChestPage cached = this.store.cached(page);
		if (cached == null) {
			if (this.store.has(page) && this.requested.add(page)) {
				this.store.load(page).thenRun(() -> this.ctx.platform().scheduler().client(() -> {
					if (!this.query.isEmpty()) {
						this.populate();
					}
				}));
			}
			return null;
		}
		PagePreview preview = this.previews.get(page);
		if (preview == null || preview.page() != cached) {
			List<GameItem> items = new ArrayList<>();
			for (CompoundTag tag : cached.items()) {
				if (!tag.isEmpty()) {
					items.add(this.ctx.client().items().fromTag(tag));
				}
			}
			preview = new PagePreview(cached, items);
			this.previews.put(page, preview);
		}
		return preview;
	}

	private void populate() {
		if (this.list == null) {
			return;
		}
		List<Row> rows = new ArrayList<>();
		List<Integer> pages = this.pages();
		if (pages.isEmpty()) {
			Row empty = new Row(ROW_HEIGHT);
			Text message = this.ctx.tr("overview.empty");
			empty.decorate((canvas, self) -> DrawUtil.text(canvas, message, self.x() + 4, self.y() + 6, Theme.FAINT));
			rows.add(empty);
		}
		for (int page : pages) {
			rows.add(this.row(page));
		}
		this.list.rows(rows);
		if (this.delete != null) {
			this.delete.enabled(!this.selected.isEmpty());
		}
	}

	private Row row(int page) {
		Row row = new Row(ROW_HEIGHT);
		row.selected(this.selected.contains(page));
		row.place(new Checkbox(Text.EMPTY, this.selected.contains(page), value -> {
			if (value) {
				this.selected.add(page);
			} else {
				this.selected.remove(page);
			}
			this.populate();
		}), 4, (ROW_HEIGHT - Checkbox.BOX) / 2, Checkbox.BOX, Checkbox.BOX);
		Text label = this.chest.label(page);
		boolean current = page == this.parent.page();
		row.decorate((canvas, self) -> this.drawRow(canvas, self, page, label, current));
		row.tooltip(() -> this.tooltip(page));
		row.onClick(input -> {
			this.parent.show(page);
			this.close();
		});
		return row;
	}

	private void drawRow(Canvas canvas, Row self, int page, Text label, boolean current) {
		int textY = self.y() + (ROW_HEIGHT - 8) / 2;
		int labelX = self.x() + 4 + Checkbox.BOX + 6;
		DrawUtil.textClipped(canvas, label, labelX, textY, LABEL_WIDTH, current ? Theme.GOLD : Theme.TEXT);
		int iconsX = labelX + LABEL_WIDTH + Theme.GAP;
		PagePreview preview = this.preview(page);
		if (preview == null) {
			Text status = this.ctx.tr(this.store.has(page) ? "overview.loading" : "overview.no_items");
			DrawUtil.text(canvas, status, iconsX, textY, Theme.FAINT);
			return;
		}
		Text count = this.ctx.tr("overview.count", preview.items().size());
		int countWidth = canvas.textWidth(count);
		DrawUtil.text(canvas, count, self.x() + self.width() - countWidth - 6, textY, Theme.FAINT);
		int room = (self.x() + self.width() - countWidth - 12 - iconsX) / ICON;
		int shown = Math.min(Math.min(MAX_ICONS, room), preview.items().size());
		for (int i = 0; i < shown; i++) {
			canvas.item(preview.items().get(i), iconsX + i * ICON, self.y() + (ROW_HEIGHT - 16) / 2);
		}
		if (preview.page().state() != PageState.OK && preview.page().state() != PageState.UPGRADED) {
			DrawUtil.text(canvas, this.ctx.tr("overview.read_only"), iconsX + shown * ICON + 4, textY, Theme.RED);
		}
	}

	private List<Text> tooltip(int page) {
		PagePreview preview = this.preview(page);
		if (preview == null || preview.items().isEmpty()) {
			return List.of();
		}
		List<Text> lines = new ArrayList<>();
		lines.add(this.chest.label(page));
		for (GameItem item : preview.items()) {
			if (lines.size() > TOOLTIP_LINES) {
				lines.add(this.ctx.tr("overview.more", preview.items().size() - TOOLTIP_LINES));
				break;
			}
			lines.add(Text.literal(item.count() + "x ").append(item.name()));
		}
		return lines;
	}

	private void selectAll() {
		for (int page : this.pages()) {
			if (this.store.interesting(page)) {
				this.selected.add(page);
			}
		}
		this.populate();
	}

	private void confirmDelete() {
		if (this.selected.isEmpty()) {
			return;
		}
		ModalUtil.confirmDanger(this, this.ctx.tr("overview.delete.title"), this.ctx.tr("overview.delete.desc", this.selected.size()), this.ctx.tr("overview.delete"), () -> {
			this.store.clear(new ArrayList<>(this.selected));
			this.selected.clear();
			this.populate();
		});
	}

	@Override
	public void onRemoved() {
		this.previews.clear();
	}
}
