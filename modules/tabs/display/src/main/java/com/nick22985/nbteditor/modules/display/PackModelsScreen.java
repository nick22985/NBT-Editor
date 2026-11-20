package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.data.PackModel;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PanelScreen;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.ItemSlot;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.RowList;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ExportUtil;
import com.nick22985.nbteditor.util.ItemModelUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PackModelsScreen extends PanelScreen {
	private static final int TREE_ROW = 12;
	private static final int INDENT = 8;
	private static final int TOGGLE = 16;
	private static final int SEARCH_MIN = 90;
	private static final int HOTBAR = 9;
	private static final String SEPARATOR = " / ";

	private final ModuleContext ctx;
	private final GameVersion version;
	private final Map<PackModel, GameItem> stacks = new HashMap<>();
	private final Map<String, Text> names = new HashMap<>();
	private final Set<ModelFolder> open = new HashSet<>();
	private ModelCatalog catalog = new ModelCatalog(List.of(), List.of());
	private List<ModelEntry> visible = List.of();
	private String query;
	private String item = "";
	private ModelGrouping grouping = ModelGrouping.FOLDER;
	private ModelSorting sorting = ModelSorting.NAME;
	private boolean list;
	private ModelFolder folder = ModelFolder.ALL;
	private ModelEntry selected;
	private int slot;
	private TextField search;
	private RowList tree;
	private ModelGridView grid;
	private ModelDetail detail;
	private final List<ItemSlot> hotbar = new ArrayList<>();
	private int statusX;

	public PackModelsScreen(ModuleContext ctx, String query) {
		super(ctx.tr("models.title"));
		this.ctx = ctx;
		this.version = CommandUtil.version(ctx);
		this.query = query == null ? "" : query;
		this.slot = ctx.client().player().selectedSlot();
		this.load();
		for (String pack : this.catalog.packs()) {
			this.open.add(ModelFolder.ALL.child(pack));
		}
	}

	public List<PackModel> shown() {
		List<PackModel> out = new ArrayList<>();
		for (ModelEntry entry : this.grid.order()) {
			out.add(entry.model());
		}
		return out;
	}

	public ModelGridView grid() {
		return this.grid;
	}

	public ModelEntry selected() {
		return this.selected;
	}

	public void view(boolean list) {
		this.list = list;
		this.rebuild();
	}

	private void load() {
		this.stacks.clear();
		this.catalog = new ModelCatalog(ItemModelUtil.entries(this.ctx.client().resources(), this.version, id -> this.ctx.client().items().isItem(id)), this.ctx.client().resources().packs());
		if (!this.catalog.exists(this.folder)) {
			this.folder = ModelFolder.ALL;
		}
		if (!this.item.isEmpty() && !this.catalog.items().contains(this.item)) {
			this.item = "";
		}
	}

	private void reload() {
		this.load();
		this.rebuild();
	}

	@Override
	protected Text subtitle() {
		return this.ctx.tr("models.shown", String.format(Locale.ROOT, "%,d", this.visible.size()));
	}

	@Override
	protected void buildPanel() {
		int toolbarY = this.bodyY;
		int right = this.bodyX + this.bodyWidth;
		Button listView = this.add(new Button(Text.EMPTY, () -> this.view(true)));
		listView.icon(PixelSprites.LIST, Theme.TEXT).selected(this.list).tooltip(this.ctx.tr("models.view.list"));
		listView.bounds(right - TOGGLE, toolbarY, TOGGLE, Theme.FIELD_HEIGHT);
		Button gridView = this.add(new Button(Text.EMPTY, () -> this.view(false)));
		gridView.icon(PixelSprites.GRID, Theme.TEXT).selected(!this.list).tooltip(this.ctx.tr("models.view.grid"));
		gridView.bounds(right - TOGGLE * 2 - 2, toolbarY, TOGGLE, Theme.FIELD_HEIGHT);
		int dropdownRight = right - TOGGLE * 2 - 2 - Theme.GAP;
		int dropdown = DrawUtil.clamp((dropdownRight - this.bodyX - Theme.GAP - SEARCH_MIN - 6) / 3, 60, 120);
		Dropdown<ModelSorting> sort = this.add(new Dropdown<>(Arrays.asList(ModelSorting.values()), this.sorting, value -> this.ctx.tr("models.sort", this.ctx.tr("models.sort." + value.id())), value -> {
			this.sorting = value;
			this.populate();
		}));
		sort.bounds(dropdownRight - dropdown, toolbarY, dropdown, Theme.FIELD_HEIGHT);
		Dropdown<ModelGrouping> group = this.add(new Dropdown<>(Arrays.asList(ModelGrouping.values()), this.grouping, value -> this.ctx.tr("models.group", this.ctx.tr("models.group." + value.id())), value -> {
			this.grouping = value;
			this.populate();
		}));
		group.bounds(dropdownRight - dropdown * 2 - 3, toolbarY, dropdown, Theme.FIELD_HEIGHT);
		List<String> items = new ArrayList<>();
		items.add("");
		items.addAll(this.catalog.items());
		Dropdown<String> itemFilter = this.add(new Dropdown<>(items, this.item, value -> this.ctx.tr("models.item", value.isEmpty() ? this.ctx.tr("models.item.all") : this.itemName(value)), value -> {
			this.item = value;
			this.populate();
		}));
		itemFilter.columns(Math.min(4, Math.max(1, (items.size() + 15) / 16)));
		itemFilter.bounds(dropdownRight - dropdown * 3 - 6, toolbarY, dropdown, Theme.FIELD_HEIGHT);
		this.search = this.add(new TextField(this.query));
		this.search.placeholder(this.ctx.tr("models.search")).onChange(value -> {
			this.query = value;
			this.grid.scroll(0);
			this.populate();
		}).onSubmit(value -> this.move(0, 1));
		this.search.bounds(this.bodyX, toolbarY, dropdownRight - dropdown * 3 - 6 - Theme.GAP - this.bodyX, Theme.FIELD_HEIGHT);
		int paneY = toolbarY + Theme.FIELD_HEIGHT + Theme.GAP;
		int paneHeight = this.bodyY + this.bodyHeight - paneY;
		int treeWidth = DrawUtil.clamp(this.bodyWidth / 6, 90, 220);
		int detailWidth = DrawUtil.clamp(this.bodyWidth / 4, 140, 240);
		this.tree = this.add(new RowList());
		this.tree.gap(0);
		this.tree.bounds(this.bodyX, paneY, treeWidth, paneHeight);
		int gridX = this.bodyX + treeWidth + Theme.GAP;
		int gridWidth = this.bodyWidth - treeWidth - detailWidth - Theme.GAP * 2;
		this.grid = this.add(new ModelGridView(this::item, this::groupTitle, this::groupTag, this::tileTooltip, this::clicked));
		this.grid.empty(this.catalog.entries().isEmpty() ? this.ctx.tr("models.empty") : this.ctx.tr("browse.none"));
		this.grid.list(this.list);
		this.grid.bounds(gridX, paneY, gridWidth, paneHeight);
		this.detail = this.add(new ModelDetail(this, this.ctx));
		this.detail.bounds(gridX + gridWidth + Theme.GAP + 2, paneY + 2, detailWidth - 2, paneHeight - 2);
		this.hotbar.clear();
		for (int i = 0; i < HOTBAR; i++) {
			int index = i;
			ItemSlot hotbarSlot = this.add(new ItemSlot(() -> this.ctx.client().player().slot(index)));
			hotbarSlot.showTooltip(false).selected(index == this.slot).onClick(input -> this.pickSlot(index));
			hotbarSlot.tooltip(() -> this.hotbarTooltip(index));
			hotbarSlot.bounds(Theme.PAD + i * Theme.SLOT, this.footerY - 1, Theme.SLOT, Theme.SLOT);
			this.hotbar.add(hotbarSlot);
		}
		this.statusX = Theme.PAD + HOTBAR * Theme.SLOT + Theme.GAP * 2;
		this.doneButton();
		this.footerButton(this.ctx.tr("models.reload"), this::reload).tooltip(this.ctx.tr("models.reload.hint"));
		this.populate();
		if (this.selected != null) {
			this.grid.reveal(this.selected);
		}
	}

	private void populate() {
		List<ModelEntry> searched = this.catalog.search(this.query, this.item);
		List<ModelEntry> shown = new ArrayList<>();
		for (ModelEntry entry : searched) {
			if (this.folder.contains(entry)) {
				shown.add(entry);
			}
		}
		this.visible = shown;
		if (this.selected != null && !shown.contains(this.selected)) {
			this.selected = null;
		}
		this.grid.crumb(this.folder.all() ? null : this.ctx.tr("models.crumb", this.folderTitle(this.folder.pack(), this.folder.path())));
		this.grid.groups(this.catalog.groups(shown, this.grouping, this.sorting), this.grouping != ModelGrouping.NONE);
		this.grid.selected(this.selected);
		List<Row> rows = new ArrayList<>();
		Row heading = new Row(TREE_ROW + 2);
		heading.decorate((canvas, self) -> DrawUtil.textClipped(canvas, this.ctx.tr("models.folders"), self.x() + 2, self.y() + 2, self.width() - 4, Theme.LABEL));
		rows.add(heading);
		for (FolderRow row : this.catalog.tree(searched, this.open)) {
			rows.add(this.folderRow(row));
		}
		this.tree.rows(rows);
		this.detail.refresh();
	}

	private Row folderRow(FolderRow entry) {
		Row row = new Row(TREE_ROW);
		row.selected(entry.folder().equals(this.folder));
		int caretX = 2 + entry.depth() * INDENT;
		row.decorate((canvas, self) -> {
			if (entry.branch() && !entry.folder().all()) {
				DrawUtil.sprite(canvas, entry.open() ? PixelSprites.DOWN : PixelSprites.RIGHT, self.x() + caretX + (entry.open() ? 0 : 1), self.y() + (entry.open() ? 5 : 4), Theme.LABEL);
			}
			String count = String.valueOf(entry.count());
			int countX = self.right() - 3 - canvas.textWidth(count);
			int color = entry.count() == 0 ? Theme.DIM : row.selected() ? Theme.TEXT : Theme.LABEL;
			DrawUtil.text(canvas, count, countX, self.y() + 2, entry.count() == 0 ? Theme.DIM : Theme.FAINT);
			int textX = self.x() + caretX + 8;
			DrawUtil.textClipped(canvas, this.folderName(entry.folder()), textX, self.y() + 2, countX - textX - 4, color);
		});
		row.onClick(input -> {
			boolean caret = input.x() < row.x() + caretX + 8;
			if (entry.branch() && !entry.folder().all() && (caret || entry.folder().equals(this.folder))) {
				if (!this.open.remove(entry.folder())) {
					this.open.add(entry.folder());
				}
			}
			if (!caret || !entry.branch()) {
				this.folder = entry.folder();
				this.grid.scroll(0);
			}
			this.populate();
		});
		return row;
	}

	private Text folderName(ModelFolder folder) {
		if (folder.all()) {
			return this.ctx.tr("models.folders.all");
		}
		if (folder.path().isEmpty()) {
			return this.packName(folder.pack());
		}
		return Text.literal(folder.name());
	}

	private Text packName(String pack) {
		return pack.isEmpty() ? this.ctx.tr("models.pack.unknown") : Text.literal(pack);
	}

	private Text folderTitle(String pack, String path) {
		String name = this.host() == null ? pack : pack.isEmpty() ? this.host().translate(this.ctx.key("models.pack.unknown")) : pack;
		return Text.literal(path.isEmpty() ? name : name + SEPARATOR + path.replace("/", SEPARATOR));
	}

	private Text groupTitle(ModelGroup group, boolean compact) {
		ModelEntry first = group.first();
		switch (this.grouping) {
			case FOLDER:
				if (compact && !first.folder().isEmpty()) {
					String real = this.catalog.realFolder(first);
					return Text.literal(real.substring(real.lastIndexOf('/') + 1));
				}
				return this.folderTitle(first.pack(), first.folder());
			case ITEM:
				return this.itemName(first.model().item());
			case PACK:
				return this.packName(first.pack());
			default:
				return Text.literal(group.key());
		}
	}

	private String groupTag(ModelGroup group) {
		if (this.host() == null) {
			return "";
		}
		ModelEntry first = group.first();
		if (this.grouping == ModelGrouping.ITEM_MODEL) {
			return this.host().translate(this.ctx.key(first.model().itemModel() != null ? "models.tag.item_model" : "models.tag.model_data"));
		}
		if (this.grouping == ModelGrouping.ITEM && first.model().itemModel() != null) {
			return this.host().translate(this.ctx.key("models.tag.pack_models"));
		}
		return "";
	}

	private Text itemName(String id) {
		return this.names.computeIfAbsent(id, key -> {
			GameItem stack = this.ctx.client().items().fromTag(ItemStackUtil.create(key, 1, this.version));
			return stack.isEmpty() ? Text.literal(key) : stack.name();
		});
	}

	public GameItem item(ModelEntry entry) {
		return this.stacks.computeIfAbsent(entry.model(), model -> this.ctx.client().items().fromTag(ItemModelUtil.item(model, this.version)));
	}

	private List<Text> tileTooltip(ModelEntry entry) {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.literal(entry.model().label()));
		if (!entry.model().model().isEmpty()) {
			lines.add(TextStyleUtil.colored(entry.model().model(), Theme.FAINT));
		}
		lines.add(TextStyleUtil.colored(this.ctx.tr("models.tile.hint"), Theme.BLUE));
		return lines;
	}

	private List<Text> hotbarTooltip(int index) {
		GameItem stack = this.ctx.client().player().slot(index);
		List<Text> lines = new ArrayList<>();
		lines.add(stack.isEmpty() ? this.ctx.tr("models.hotbar.empty") : stack.name());
		lines.add(TextStyleUtil.colored(this.ctx.tr("models.hotbar"), Theme.FAINT));
		return lines;
	}

	private void pickSlot(int index) {
		this.slot = index;
		for (int i = 0; i < this.hotbar.size(); i++) {
			this.hotbar.get(i).selected(i == index);
		}
		this.detail.refresh();
	}

	private void clicked(ModelEntry entry, MouseInput input) {
		if (input.middle()) {
			this.copyId(entry);
			return;
		}
		if (!input.left()) {
			return;
		}
		this.select(entry);
		if (input.modifiers().shift()) {
			this.give(entry);
		}
	}

	public String realFolder(ModelEntry entry) {
		return this.catalog.realFolder(entry);
	}

	public void select(ModelEntry entry) {
		this.selected = entry;
		this.grid.selected(entry);
		if (entry != null) {
			this.grid.reveal(entry);
		}
		this.detail.refresh();
	}

	private void move(int dx, int dy) {
		ModelEntry next = this.grid.neighbour(this.selected, dx, dy);
		if (next != null) {
			this.setFocus(null);
			this.select(next);
		}
	}

	public boolean creative() {
		return this.ctx.client().player().creative();
	}

	public String giveCommand(ModelEntry entry) {
		return ExportUtil.giveCommand(ItemModelUtil.item(entry.model(), this.version), this.version, CommandUtil.dialect(this.version));
	}

	public Text applyBlock(ModelEntry entry) {
		if (!this.creative()) {
			return this.ctx.tr("models.hint.creative");
		}
		GameItem held = this.ctx.client().player().slot(this.slot);
		if (held.isEmpty()) {
			return this.ctx.tr("models.hint.empty");
		}
		if (!Feature.ITEM_MODEL_COMPONENT.in(this.version) && !held.id().equals(entry.model().item())) {
			return this.ctx.tr("models.hint.base", this.itemName(entry.model().item()));
		}
		return null;
	}

	public Text hint(ModelEntry entry) {
		return this.applyBlock(entry);
	}

	public void give(ModelEntry entry) {
		if (!this.creative()) {
			this.toast(this.ctx.tr("models.hint.creative"));
			return;
		}
		boolean given = this.ctx.client().player().give(this.ctx.client().items().fromTag(ItemModelUtil.item(entry.model(), this.version)));
		this.toast(given ? this.ctx.tr("models.gave", entry.model().label()) : this.ctx.tr("models.full"));
	}

	public void apply(ModelEntry entry) {
		Text blocked = this.applyBlock(entry);
		if (blocked != null) {
			this.toast(blocked);
			return;
		}
		PlayerService player = this.ctx.client().player();
		GameItem held = player.slot(this.slot);
		CompoundTag tag = held.tag().copy();
		PackModel model = entry.model();
		if (Feature.ITEM_MODEL_COMPONENT.in(this.version)) {
			if (model.itemModel() != null) {
				ItemStackUtil.setComponent(tag, ItemModelUtil.ITEM_MODEL, StringTag.of(model.itemModel()));
			} else if (!held.id().equals(model.item())) {
				ItemStackUtil.setComponent(tag, ItemModelUtil.ITEM_MODEL, StringTag.of(model.item()));
			} else {
				ItemStackUtil.setComponent(tag, ItemModelUtil.ITEM_MODEL, null);
			}
		}
		if (model.value() != null) {
			ItemFields.CUSTOM_MODEL_DATA.write(tag, this.version, model.value());
		} else {
			ItemFields.CUSTOM_MODEL_DATA.remove(tag, this.version);
		}
		if (player.setSlot(this.slot, this.ctx.client().items().fromTag(tag))) {
			this.toast(this.ctx.tr("models.applied", model.label(), held.name()));
		}
		this.detail.refresh();
	}

	public void copyId(ModelEntry entry) {
		this.ctx.client().system().setClipboard(entry.model().label());
		this.toast(this.ctx.tr("models.copied"));
	}

	public void copyGive(ModelEntry entry) {
		this.ctx.client().system().setClipboard("/" + this.giveCommand(entry));
		this.toast(this.ctx.tr("models.copied_give"));
	}

	private void toast(Text message) {
		this.ctx.client().screens().toast(this.ctx.tr("models.title"), message);
	}

	@Override
	protected void onTick() {
		this.detail.refresh();
	}

	@Override
	protected void renderPanelBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.rect(canvas, this.detail.x() - Theme.GAP / 2 - 1, this.detail.y() - 2, 1, this.detail.height() + 2, Theme.SECTION_LINE);
		Text status = this.ctx.tr("models.status", String.format(Locale.ROOT, "%,d", this.catalog.entries().size()), this.catalog.packs().size());
		DrawUtil.textClipped(canvas, status, this.statusX, this.footerY + 4, this.footerLeftWidth() - this.statusX + Theme.PAD, Theme.FAINT);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.topOverlay() != null) {
			return super.keyPressed(input);
		}
		boolean typing = this.typing();
		if (input.is(Key.UP) || input.is(Key.DOWN)) {
			this.move(0, input.is(Key.UP) ? -1 : 1);
			return true;
		}
		if (!typing && (input.is(Key.LEFT) || input.is(Key.RIGHT))) {
			this.move(input.is(Key.LEFT) ? -1 : 1, 0);
			return true;
		}
		if (!typing && input.isEnter() && this.selected != null && this.focusedWidget() == null) {
			this.give(this.selected);
			return true;
		}
		return super.keyPressed(input);
	}
}
