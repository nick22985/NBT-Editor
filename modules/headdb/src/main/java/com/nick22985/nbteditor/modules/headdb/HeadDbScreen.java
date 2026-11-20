package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.inventory.InventoryScreen;
import com.nick22985.nbteditor.ui.inventory.InventoryStatus;
import com.nick22985.nbteditor.ui.inventory.ListSection;
import com.nick22985.nbteditor.ui.inventory.PlayerSection;
import com.nick22985.nbteditor.ui.inventory.SlotMode;
import com.nick22985.nbteditor.ui.inventory.SlotRef;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadDbScreen extends InventoryScreen {
	private static final int COLUMNS = 9;
	private static final int ROWS = 6;
	private static final int PAGE_SIZE = COLUMNS * ROWS;

	private final HeadDatabase database;
	private final HeadItems heads;
	private ListSection list;
	private HeadCategory category = HeadCategory.ALL;
	private String query;
	private List<Head> results = List.of();
	private List<Head> shown = List.of();
	private int page;
	private int version = -1;

	public HeadDbScreen(ModuleContext ctx, HeadDatabase database, String query) {
		super(ctx, ctx.tr("title"));
		this.database = database;
		this.heads = new HeadItems(ctx);
		this.query = query == null ? "" : query;
		this.layoutSections();
		this.load(false);
	}

	public List<Head> shown() {
		return this.shown;
	}

	public ListSection list() {
		return this.list;
	}

	private void layoutSections() {
		this.clearSections();
		this.list = this.addSection(new ListSection(this.ctx.tr(this.category.langKey()), ContainerLayout.grid(COLUMNS, ROWS), List.of(), this.items().empty()));
		ItemTargets targets = this.ctx.services().find(ItemTargets.class).orElse(null);
		this.addSection(new PlayerSection(this.ctx.tr("inventory"), this.player(), targets));
	}

	private void load(boolean refresh) {
		this.database.load(refresh);
		this.search();
	}

	@Override
	protected void onTick() {
		if (this.version != this.database.version()) {
			this.search();
		}
	}

	private void search() {
		this.version = this.database.version();
		this.results = this.database.search(this.category, this.query);
		this.show(this.page);
	}

	private int pages() {
		return Math.max(1, (this.results.size() + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	private void show(int next) {
		this.page = Math.max(0, Math.min(this.pages() - 1, next));
		int from = this.page * PAGE_SIZE;
		this.shown = this.results.subList(Math.min(from, this.results.size()), Math.min(from + PAGE_SIZE, this.results.size()));
		List<GameItem> items = new ArrayList<>();
		for (Head head : this.shown) {
			items.add(this.heads.create(head));
		}
		this.list.replace(items);
		this.list.mode(this.player().creative() ? SlotMode.SOURCE : SlotMode.READ_ONLY);
	}

	@Override
	protected void buildSide() {
		this.sideWidget(new TextField(this.query).placeholder(this.ctx.tr("search")).onChange(value -> {
			this.query = value;
			this.page = 0;
			this.search();
		}));
		this.sideButton(this.ctx.tr("category", this.ctx.tr(this.category.langKey())), this::pickCategory);
		this.sideButton(this.ctx.tr("player"), this::pickPlayer).tooltip(this.ctx.tr("player.hint"));
		this.sideButton(this.ctx.tr("previous"), () -> this.show(this.page - 1));
		this.sideButton(this.ctx.tr("next"), () -> this.show(this.page + 1));
		this.sideButton(this.ctx.tr("refresh"), () -> this.load(true)).tooltip(this.ctx.tr("refresh.hint"));
		this.sideButton(this.ctx.tr("close"), this::close);
	}

	@Override
	protected Text sideText() {
		return this.ctx.tr("hint");
	}

	private void pickPlayer() {
		ModalUtil.input(this, this.ctx.tr("player.title"), "", value -> PlayerHeads.NAME.matcher(value).matches() ? null : this.ctx.tr("player.invalid"), name -> {
			this.ctx.client().screens().toast(this.ctx.tr("title"), this.ctx.tr("player.looking", name));
			this.database.player(name).whenComplete((head, error) -> this.ctx.platform().scheduler().client(() -> this.playerFound(name, head == null ? null : head.orElse(null), error)));
		});
	}

	private void playerFound(String name, Head head, Throwable error) {
		if (error != null) {
			this.ctx.log().warn("Could not look up player " + name, error);
			this.ctx.client().screens().toast(this.ctx.tr("title"), this.ctx.tr("player.failed"));
			return;
		}
		if (head == null) {
			this.ctx.client().screens().toast(this.ctx.tr("title"), this.ctx.tr("player.missing", name));
			return;
		}
		this.ctx.client().screens().toast(this.ctx.tr("title"), this.ctx.tr("player.added", head.name()));
		if (this.ctx.client().screens().current() != this) {
			return;
		}
		this.category = HeadCategory.PLAYERS;
		this.query = "";
		this.page = 0;
		this.layoutSections();
		this.search();
		this.rebuild();
	}

	private void pickCategory() {
		ModalUtil.pick(this, this.ctx.tr("category.title"), Arrays.asList(HeadCategory.values()), value -> this.ctx.tr(value.langKey()), value -> String.format(Locale.ROOT, "%,d", this.database.count(value)), value -> {
			this.category = value;
			this.page = 0;
			this.layoutSections();
			this.search();
			this.rebuild();
		});
	}

	@Override
	protected InventoryStatus status() {
		switch (this.database.state()) {
			case IDLE:
				return InventoryStatus.of(this.ctx.tr("status.loading"), Theme.FAINT);
			case LOADING:
				return this.loadingStatus();
			case FAILED:
				return new InventoryStatus(this.ctx.tr("status.failed"), Theme.RED, PixelSprites.CROSS);
			default:
				Text text = this.resultsText();
				return this.database.state() == HeadDbState.OFFLINE ? new InventoryStatus(this.ctx.tr("status.offline", text), Theme.YELLOW, PixelSprites.LOCK) : InventoryStatus.of(text, Theme.FAINT);
		}
	}

	private Text resultsText() {
		return this.ctx.tr("status.results", String.format(Locale.ROOT, "%,d", this.results.size()), this.page + 1, this.pages());
	}

	private InventoryStatus loadingStatus() {
		int total = this.database.downloads();
		if (!this.database.empty()) {
			return InventoryStatus.of(total == 0 ? this.resultsText() : this.ctx.tr("status.updating", this.resultsText(), this.database.downloaded(), total), Theme.FAINT);
		}
		return InventoryStatus.of(total == 0 ? this.ctx.tr("status.loading") : this.ctx.tr("status.progress", this.database.downloaded(), total), Theme.FAINT);
	}

	@Override
	protected boolean slotPressed(SlotRef ref, MouseInput input) {
		if (ref.section() != this.list) {
			return false;
		}
		if (ref.index() >= this.shown.size()) {
			return true;
		}
		Head head = this.shown.get(ref.index());
		if (input.middle()) {
			this.ctx.client().system().setClipboard(head.value());
			this.ctx.client().screens().toast(this.ctx.tr("title"), this.ctx.tr("copied"));
			return true;
		}
		if (input.right()) {
			boolean added = this.database.toggleFavorite(head);
			this.ctx.client().screens().toast(this.ctx.tr("title"), this.ctx.tr(added ? "favorited" : "unfavorited"));
			if (this.category == HeadCategory.FAVORITES) {
				this.search();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.topOverlay() == null && !this.typing() && (input.is(Key.PAGE_UP) || input.is(Key.PAGE_DOWN))) {
			this.show(this.page + (input.is(Key.PAGE_DOWN) ? 1 : -1));
			return true;
		}
		return super.keyPressed(input);
	}
}
