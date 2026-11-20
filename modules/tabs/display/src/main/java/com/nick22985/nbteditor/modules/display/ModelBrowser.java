package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.ClientPlatform;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.util.TextStyleUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelBrowser<T> {
	private static final int COLUMNS = 9;
	private static final int ROWS = 6;
	private static final int PAGE = COLUMNS * ROWS;

	private final ModuleContext ctx;
	private final ClientPlatform client;
	private final Text title;
	private final List<T> values;
	private final Function<T, Text> labeler;
	private final Function<T, String> detail;
	private final Function<T, CompoundTag> preview;
	private final Consumer<T> onPick;
	private final List<T> shown = new ArrayList<>();
	private final List<GameItem> previews = new ArrayList<>();
	private Text note;
	private ModalPanel panel;
	private ModelSearchGrid content;
	private Button previous;
	private Button next;
	private int page;

	public ModelBrowser(ModuleContext ctx, ClientPlatform client, Text title, List<T> values, Function<T, Text> labeler, Function<T, String> detail, Function<T, CompoundTag> preview, Consumer<T> onPick) {
		this.ctx = ctx;
		this.client = client;
		this.title = title;
		this.values = List.copyOf(values);
		this.labeler = labeler;
		this.detail = detail;
		this.preview = preview;
		this.onPick = onPick;
	}

	public ModelBrowser<T> note(Text note) {
		this.note = note;
		return this;
	}

	public void open(UiScreen screen) {
		this.panel = new ModalPanel(this.title, COLUMNS * Theme.SLOT + 40);
		if (this.note != null) {
			this.panel.message(this.note);
		}
		this.content = new ModelSearchGrid(COLUMNS, PAGE, this::previewAt, this::tooltip);
		this.panel.content(this.content, this.content.preferredHeight());
		this.content.search().placeholder(this.ctx.tr("browse.search"));
		this.content.search().onChange(query -> this.filter());
		this.content.search().onSubmit(query -> this.pick(0));
		this.content.grid().onClick((index, input) -> this.pick(index));
		this.previous = this.panel.action(this.ctx.tr("model_data.browse.previous"), () -> this.page(this.page - 1));
		this.next = this.panel.action(this.ctx.tr("model_data.browse.next"), () -> this.page(this.page + 1));
		this.panel.closingAction(Text.translatable("gui.cancel"), null);
		this.filter();
		screen.openOverlay(this.panel);
		screen.setFocus(this.content.search());
	}

	private boolean matches(T value, String query) {
		if (query.isEmpty() || this.labeler.apply(value).plain().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		String extra = this.detail.apply(value);
		return extra != null && extra.toLowerCase(Locale.ROOT).contains(query);
	}

	private void filter() {
		String query = this.content.search().value().trim().toLowerCase(Locale.ROOT);
		this.shown.clear();
		for (T value : this.values) {
			if (this.matches(value, query)) {
				this.shown.add(value);
			}
		}
		this.page(0);
	}

	private int pages() {
		return Math.max(1, (this.shown.size() + PAGE - 1) / PAGE);
	}

	private int start() {
		return this.page * PAGE;
	}

	private void page(int page) {
		this.page = Math.max(0, Math.min(page, this.pages() - 1));
		this.previews.clear();
		for (int i = this.start(); i < Math.min(this.shown.size(), this.start() + PAGE); i++) {
			this.previews.add(this.client.items().fromTag(this.preview.apply(this.shown.get(i))));
		}
		this.previous.enabled(this.page > 0);
		this.next.enabled(this.page < this.pages() - 1);
		this.panel.hint(this.shown.isEmpty() ? this.ctx.tr("browse.none") : this.ctx.tr("browse.page", this.shown.size(), this.page + 1, this.pages()));
	}

	private void pick(int index) {
		if (index < this.previews.size()) {
			this.panel.close();
			this.onPick.accept(this.shown.get(this.start() + index));
		}
	}

	private GameItem previewAt(int index) {
		return index < this.previews.size() ? this.previews.get(index) : null;
	}

	private List<Text> tooltip(int index) {
		if (index >= this.previews.size()) {
			return List.of();
		}
		T value = this.shown.get(this.start() + index);
		List<Text> lines = new ArrayList<>();
		lines.add(this.labeler.apply(value));
		String extra = this.detail.apply(value);
		if (extra != null && !extra.isEmpty()) {
			lines.add(TextStyleUtil.colored(extra, Theme.FAINT));
		}
		return lines;
	}
}
