package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestSlotTarget implements Target {
	private final ModuleContext ctx;
	private final ClientChest chest;
	private final int page;
	private final int slot;
	private Text name;

	public ClientChestSlotTarget(ModuleContext ctx, ClientChest chest, int page, int slot) {
		this.ctx = ctx;
		this.chest = chest;
		this.page = page;
		this.slot = slot;
	}

	@Override
	public String id() {
		return "client_chest/page:" + this.page + "/slot:" + this.slot;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ITEM;
	}

	@Override
	public Text displayName() {
		return this.name == null ? this.ctx.tr("slot", this.slot + 1) : this.name;
	}

	@Override
	public Text source() {
		return this.ctx.tr("slot.source", this.chest.label(this.page), this.slot + 1);
	}

	@Override
	public Set<TargetCapability> capabilities() {
		Set<TargetCapability> capabilities = EnumSet.of(TargetCapability.READ, TargetCapability.PREVIEW_ITEM);
		ClientChestPage cached = this.chest.store().cached(this.page);
		if (cached == null || cached.state().writable()) {
			capabilities.add(TargetCapability.WRITE);
		}
		return capabilities;
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		return this.chest.store().load(this.page).thenApply(page -> {
			CompoundTag item = page.item(this.slot);
			this.name = item.isEmpty() ? null : this.ctx.client().items().fromTag(item).name();
			return item;
		});
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		return this.chest.store().load(this.page).thenAccept(page -> {
			if (!page.state().writable()) {
				throw new TargetException(this.ctx.tr("read_only"));
			}
			List<CompoundTag> items = new ArrayList<>(page.items());
			items.set(this.slot, tag.copy());
			this.chest.store().save(this.page, items);
		});
	}

	@Override
	public GameItem previewItem(CompoundTag tag) {
		return this.ctx.client().items().fromTag(tag);
	}
}
