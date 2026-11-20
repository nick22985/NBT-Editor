package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.container.ItemStash;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChest implements ItemStash {
	private static final int INSERT_SEARCH = 1000;

	private final ModuleContext ctx;
	private final ClientChestStore store;
	private final ClientChestSettings settings;
	private int lastPage;

	public ClientChest(ModuleContext ctx, ClientChestStore store, ClientChestSettings settings) {
		this.ctx = ctx;
		this.store = store;
		this.settings = settings;
	}

	public ClientChestStore store() {
		return this.store;
	}

	public ClientChestSettings settings() {
		return this.settings;
	}

	public int pages() {
		return this.settings.pages();
	}

	public boolean unlimited() {
		return this.settings.unlimited();
	}

	public int clamp(int page) {
		return Math.max(0, Math.min(this.pages() - 1, page));
	}

	public int lastPage() {
		return this.clamp(this.lastPage);
	}

	public void lastPage(int page) {
		this.lastPage = this.clamp(page);
	}

	public Text label(int page) {
		String name = this.store.name(page);
		return name == null ? this.ctx.tr("page", page + 1) : this.ctx.tr("page.named", page + 1, name);
	}

	public Integer resolve(String query) {
		String value = query.trim();
		if (value.isEmpty()) {
			return null;
		}
		try {
			int page = Integer.parseInt(value) - 1;
			return page >= 0 && page < this.pages() ? page : null;
		} catch (NumberFormatException e) {
			return this.store.find(value);
		}
	}

	public void open() {
		this.open(this.lastPage());
	}

	public void open(int page) {
		this.lastPage(page);
		this.ctx.platform().scheduler().client(() -> this.ctx.client().screens().open(new ClientChestScreen(this.ctx, this, this.lastPage())));
	}

	public CompletableFuture<List<CompoundTag>> read(int page) {
		return this.store.load(this.clamp(page)).thenApply(ClientChestPage::items);
	}

	public CompletableFuture<Boolean> write(int page, List<CompoundTag> items) {
		int index = this.clamp(page);
		return this.store.load(index).thenApply(loaded -> {
			if (!loaded.state().writable()) {
				return false;
			}
			this.store.save(index, items);
			return true;
		});
	}

	@Override
	public CompletableFuture<Integer> insert(GameItem item) {
		if (!InventoryUtil.present(item)) {
			return CompletableFuture.completedFuture(-1);
		}
		return this.insert(item.tag().copy(), this.lastPage(), 0);
	}

	private CompletableFuture<Integer> insert(CompoundTag tag, int page, int searched) {
		if (page >= this.pages() || searched >= INSERT_SEARCH) {
			return CompletableFuture.completedFuture(-1);
		}
		return this.store.load(page).thenCompose(loaded -> {
			if (loaded.state().writable()) {
				List<CompoundTag> items = new ArrayList<>(loaded.items());
				for (int slot = 0; slot < items.size(); slot++) {
					if (items.get(slot).isEmpty()) {
						items.set(slot, tag);
						this.store.save(page, items);
						return CompletableFuture.completedFuture(page);
					}
				}
			}
			return this.insert(tag, page + 1, searched + 1);
		});
	}
}
