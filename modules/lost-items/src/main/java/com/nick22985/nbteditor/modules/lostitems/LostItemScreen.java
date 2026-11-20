package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.inventory.InventoryScreen;
import com.nick22985.nbteditor.ui.inventory.InventoryStatus;
import com.nick22985.nbteditor.ui.inventory.ListSection;
import com.nick22985.nbteditor.ui.inventory.PlayerSection;
import com.nick22985.nbteditor.ui.inventory.SlotMode;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItemScreen extends InventoryScreen {
	private static final int COLUMNS = 9;
	private static final int ROWS = 6;
	private static final int PAGE = COLUMNS * ROWS;

	private final LostItemHistory history;
	private final LostItemSettings settings;
	private final ListSection list;
	private LostItemSource source;
	private int page;
	private int total;

	public LostItemScreen(ModuleContext ctx, LostItemHistory history, LostItemSettings settings, LostItemSource source) {
		super(ctx, ctx.tr("title"));
		this.history = history;
		this.settings = settings;
		this.source = source;
		this.list = this.addSection(new ListSection(ctx.tr("source." + source.serializedName()), ContainerLayout.grid(COLUMNS, ROWS), this.pageItems(), this.items().empty()));
		this.list.mode(this.player().creative() ? SlotMode.SOURCE : SlotMode.READ_ONLY);
		ItemTargets targets = ctx.services().find(ItemTargets.class).orElse(null);
		this.addSection(new PlayerSection(ctx.tr("inventory"), this.player(), targets));
	}

	private List<GameItem> pageItems() {
		List<LostItemEntry> entries = this.history.entries(this.source);
		this.total = entries.size();
		int pages = this.pages();
		if (this.page >= pages) {
			this.page = pages - 1;
		}
		List<GameItem> items = new ArrayList<>();
		for (int i = 0; i < PAGE; i++) {
			int index = this.page * PAGE + i;
			items.add(index < entries.size() ? this.items().fromTag(entries.get(index).item()) : this.items().empty());
		}
		return items;
	}

	private int pages() {
		return Math.max(1, (this.total + PAGE - 1) / PAGE);
	}

	private void refresh() {
		this.list.replace(this.pageItems());
		this.rebuild();
	}

	private void select(LostItemSource next) {
		this.source = next;
		this.page = 0;
		this.refresh();
	}

	@Override
	protected void buildSide() {
		for (LostItemSource option : LostItemSource.values()) {
			Text label = this.ctx.tr("source." + option.serializedName());
			if (option == this.source) {
				label = this.ctx.tr("selected", label);
			}
			this.sideButton(label, () -> this.select(option)).tooltip(this.settings.enabled(option) ? this.ctx.tr("source." + option.serializedName() + ".desc") : this.ctx.tr("disabled"));
		}
		if (this.pages() > 1) {
			this.sideButton(this.ctx.tr("previous"), () -> this.turn(-1)).enabled(this.page > 0);
			this.sideButton(this.ctx.tr("next"), () -> this.turn(1)).enabled(this.page < this.pages() - 1);
		}
		this.sideButton(this.ctx.tr("clear"), this::confirmClear).enabled(this.total > 0);
		this.sideButton(this.ctx.tr("close"), this::close);
	}

	private void turn(int delta) {
		this.page = Math.max(0, Math.min(this.pages() - 1, this.page + delta));
		this.refresh();
	}

	private void confirmClear() {
		Text name = this.ctx.tr("source." + this.source.serializedName());
		ModalUtil.confirmDanger(this, this.ctx.tr("clear.title"), this.ctx.tr("clear.desc", name), this.ctx.tr("clear"), () -> {
			this.history.clear(this.source);
			this.history.save();
			this.refresh();
		});
	}

	@Override
	protected Text sideText() {
		return this.ctx.tr(this.player().creative() ? "hint.creative" : "hint.survival");
	}

	@Override
	protected InventoryStatus status() {
		if (this.pages() > 1) {
			return InventoryStatus.of(this.ctx.tr("page", this.page + 1, this.pages()), Theme.FAINT);
		}
		return InventoryStatus.of(this.ctx.tr("order"), Theme.FAINT);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.plain(Key.E) && this.topOverlay() == null) {
			this.close();
			return true;
		}
		return super.keyPressed(input);
	}
}
