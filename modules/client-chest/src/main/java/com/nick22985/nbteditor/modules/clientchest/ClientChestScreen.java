package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.hook.LostItemReason;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.inventory.InventoryScreen;
import com.nick22985.nbteditor.ui.inventory.InventoryStatus;
import com.nick22985.nbteditor.ui.inventory.ListSection;
import com.nick22985.nbteditor.ui.inventory.PlayerSection;
import com.nick22985.nbteditor.ui.inventory.SlotMode;
import com.nick22985.nbteditor.util.InventoryUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestScreen extends InventoryScreen {
	private static final int COLUMNS = 9;
	private static final int ROWS = 6;

	private final ClientChest chest;
	private final ClientChestStore store;
	private ListSection list;
	private ClientChestPage shown;
	private int page;
	private boolean copying;
	private boolean dirty;
	private InventoryStatus status;

	public ClientChestScreen(ModuleContext ctx, ClientChest chest, int page) {
		super(ctx, ctx.tr("title"));
		this.chest = chest;
		this.store = chest.store();
		this.page = chest.clamp(page);
		this.layoutSections();
		this.load();
	}

	public int page() {
		return this.page;
	}

	private void layoutSections() {
		this.clearSections();
		this.list = this.addSection(new ListSection(this.chest.label(this.page), ContainerLayout.grid(COLUMNS, ROWS), List.of(), this.items().empty()));
		this.list.onChange(this::markDirty);
		int index = this.page;
		this.list.targets(slot -> new ClientChestSlotTarget(this.ctx, this.chest, index, slot));
		this.list.mode(SlotMode.READ_ONLY);
		ItemTargets targets = this.ctx.services().find(ItemTargets.class).orElse(null);
		this.addSection(new PlayerSection(this.ctx.tr("inventory"), this.player(), targets));
		if (this.built()) {
			this.rebuild();
		}
	}

	private void load() {
		this.shown = null;
		this.status = InventoryStatus.of(this.ctx.tr("status.loading"), Theme.FAINT);
		int requested = this.page;
		CompletableFuture<ClientChestPage> future = this.store.load(requested);
		ClientChestPage ready = future.getNow(null);
		if (ready != null) {
			this.apply(ready);
			return;
		}
		future.whenComplete((loaded, error) -> this.ctx.platform().scheduler().client(() -> {
			if (this.page != requested || this.shown != null) {
				return;
			}
			if (error != null) {
				this.ctx.log().warn("Could not load client chest page " + (requested + 1), error);
				this.status = new InventoryStatus(this.ctx.tr("status.failed"), Theme.RED, PixelSprites.CROSS);
				return;
			}
			this.apply(loaded);
		}));
	}

	private void apply(ClientChestPage loaded) {
		this.shown = loaded;
		List<GameItem> items = new ArrayList<>();
		for (CompoundTag tag : loaded.items()) {
			items.add(tag.isEmpty() ? this.items().empty() : this.items().fromTag(tag));
		}
		this.list.replace(items);
		this.list.mode(this.mode());
		this.status = this.stateStatus(loaded);
		if (this.built()) {
			this.rebuild();
		}
	}

	private SlotMode mode() {
		if (this.shown == null) {
			return SlotMode.READ_ONLY;
		}
		if (!this.shown.state().writable()) {
			return this.player().creative() ? SlotMode.SOURCE : SlotMode.READ_ONLY;
		}
		return this.copying && this.player().creative() ? SlotMode.SOURCE : SlotMode.NORMAL;
	}

	private InventoryStatus stateStatus(ClientChestPage loaded) {
		switch (loaded.state()) {
			case UPGRADED:
				return new InventoryStatus(this.ctx.tr("status.upgraded"), Theme.YELLOW, PixelSprites.UP);
			case NEWER:
				return new InventoryStatus(this.ctx.tr("status.newer"), Theme.RED, PixelSprites.LOCK);
			case BROKEN:
				return new InventoryStatus(this.ctx.tr("status.broken"), Theme.RED, PixelSprites.CROSS);
			default:
				return null;
		}
	}

	private boolean writable() {
		return this.shown != null && this.shown.state().writable();
	}

	private void markDirty() {
		this.dirty = true;
	}

	private void save() {
		if (!this.dirty || !this.writable()) {
			return;
		}
		this.dirty = false;
		List<CompoundTag> tags = new ArrayList<>();
		for (GameItem item : this.list.items()) {
			tags.add(InventoryUtil.present(item) ? item.tag().copy() : new CompoundTag());
		}
		this.store.save(this.page, tags);
		this.shown = this.store.cached(this.page);
		this.status = null;
	}

	private void go(int next) {
		int target = this.chest.clamp(next);
		if (target == this.page) {
			return;
		}
		this.save();
		this.page = target;
		this.chest.lastPage(target);
		this.layoutSections();
		this.load();
	}

	private void turn(boolean forward, boolean jump) {
		if (jump) {
			Integer near = this.store.nearby(this.page, forward);
			this.go(near == null ? forward ? this.page : 0 : near);
			return;
		}
		this.go(this.page + (forward ? 1 : -1));
	}

	private boolean shift() {
		return this.host() != null && this.host().modifiers().shift();
	}

	@Override
	protected void buildSide() {
		this.sideButton(this.ctx.tr("previous"), () -> this.turn(false, this.shift())).enabled(this.page > 0).tooltip(this.ctx.tr("turn.hint"));
		this.sideButton(this.ctx.tr("next"), () -> this.turn(true, this.shift())).enabled(this.page < this.chest.pages() - 1).tooltip(this.ctx.tr("turn.hint"));
		this.sideButton(this.ctx.tr("goto"), this::promptGoto);
		this.sideButton(this.ctx.tr("rename"), this::promptRename);
		this.sideButton(this.ctx.tr("overview"), this::openOverview);
		if (this.player().creative() && this.writable()) {
			this.sideButton(this.ctx.tr(this.copying ? "mode.copy" : "mode.normal"), this::toggleMode).tooltip(this.ctx.tr("mode.hint"));
		}
		this.sideButton(this.ctx.tr("reload"), this::reload).tooltip(this.ctx.tr("reload.hint"));
		this.sideButton(this.ctx.tr("clear"), this::confirmClear).enabled(this.writable() && this.list.items().stream().anyMatch(InventoryUtil::present));
		this.sideButton(this.ctx.tr("close"), this::close);
	}

	private void toggleMode() {
		this.copying = !this.copying;
		this.list.mode(this.mode());
		this.rebuild();
	}

	private void promptGoto() {
		ModalUtil.input(this, this.ctx.tr("goto.title"), String.valueOf(this.page + 1), value -> this.chest.resolve(value) == null ? this.ctx.tr("goto.invalid", this.chest.unlimited() ? "∞" : String.valueOf(this.chest.pages())) : null, value -> this.go(this.chest.resolve(value)));
	}

	private void promptRename() {
		String current = this.store.name(this.page);
		ModalUtil.input(this, this.ctx.tr("rename.title", this.page + 1), current == null ? "" : current, value -> {
			Integer owner = value.trim().isEmpty() ? null : this.store.find(value);
			return owner != null && owner != this.page ? this.ctx.tr("rename.taken", owner + 1) : null;
		}, value -> {
			this.save();
			if (this.store.rename(this.page, value)) {
				this.layoutSections();
				this.apply(this.shown == null ? this.store.cached(this.page) : this.shown);
			}
		});
	}

	public void openOverview() {
		this.save();
		this.ctx.client().screens().open(new ClientChestOverviewScreen(this.ctx, this.chest, this));
	}

	private void reload() {
		this.dirty = false;
		this.store.forget(this.page);
		this.load();
	}

	private void confirmClear() {
		ModalUtil.confirmDanger(this, this.ctx.tr("clear.title"), this.ctx.tr("clear.desc", this.chest.label(this.page)), this.ctx.tr("clear"), () -> {
			for (GameItem item : this.list.items()) {
				this.lose(item, LostItemReason.DELETED);
			}
			this.dirty = false;
			this.store.save(this.page, List.of());
			this.apply(this.store.cached(this.page));
		});
	}

	public void show(int next) {
		if (next == this.page) {
			this.reload();
			return;
		}
		this.dirty = false;
		this.page = this.chest.clamp(next);
		this.chest.lastPage(this.page);
		this.layoutSections();
		this.load();
	}

	@Override
	protected Text sideText() {
		if (this.chest.unlimited()) {
			return null;
		}
		return this.ctx.tr("hint.limited", this.page + 1, this.chest.pages());
	}

	@Override
	protected InventoryStatus status() {
		return this.status;
	}

	@Override
	protected void onTick() {
		if (this.dirty) {
			this.save();
			return;
		}
		if (this.shown != null) {
			ClientChestPage cached = this.store.cached(this.page);
			if (cached != null && cached != this.shown) {
				this.apply(cached);
			}
		}
	}

	@Override
	public void onShown() {
		if (!this.dirty && this.shown != null) {
			ClientChestPage cached = this.store.cached(this.page);
			if (cached == null) {
				this.load();
			} else if (cached != this.shown) {
				this.apply(cached);
			}
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.topOverlay() == null) {
			if (input.plain(Key.E)) {
				this.close();
				return true;
			}
			if (input.is(Key.PAGE_UP) || input.is(Key.PAGE_DOWN)) {
				this.turn(input.is(Key.PAGE_DOWN), input.modifiers().shift());
				return true;
			}
		}
		return super.keyPressed(input);
	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		this.save();
	}
}
