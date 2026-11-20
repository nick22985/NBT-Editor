package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.target.WorldBackends;
import com.nick22985.nbteditor.text.Text;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotItemTarget implements Target {
	private final ModuleContext ctx;
	private final int slot;

	public SlotItemTarget(ModuleContext ctx, int slot) {
		this.ctx = ctx;
		this.slot = slot;
	}

	public int slot() {
		return this.slot;
	}

	private PlayerService player() {
		return this.ctx.client().player();
	}

	private Optional<WorldBackend> backend() {
		return this.ctx.services().get(WorldBackends.class).best(SubjectKind.PLAYER, TargetCapability.WRITE);
	}

	@Override
	public String id() {
		return "item:slot:" + this.slot;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ITEM;
	}

	@Override
	public Text displayName() {
		GameItem item = this.player().present() ? this.player().slot(this.slot) : null;
		return item == null || item.isEmpty() ? this.ctx.tr("empty_slot") : item.name();
	}

	@Override
	public int inventorySlot() {
		return this.slot;
	}

	@Override
	public Text source() {
		if (this.slot == this.player().selectedSlot()) {
			return this.ctx.tr("slot.main_hand");
		}
		if (this.slot < 9) {
			return this.ctx.tr("slot.hotbar", this.slot + 1);
		}
		if (this.slot < PlayerService.FEET_SLOT) {
			return this.ctx.tr("slot.inventory", this.slot - 8);
		}
		if (this.slot == PlayerService.FEET_SLOT) {
			return this.ctx.tr("slot.feet");
		}
		if (this.slot == PlayerService.LEGS_SLOT) {
			return this.ctx.tr("slot.legs");
		}
		if (this.slot == PlayerService.CHEST_SLOT) {
			return this.ctx.tr("slot.chest");
		}
		if (this.slot == PlayerService.HEAD_SLOT) {
			return this.ctx.tr("slot.head");
		}
		if (this.slot == PlayerService.OFFHAND_SLOT) {
			return this.ctx.tr("slot.offhand");
		}
		return this.ctx.tr("slot.other", this.slot);
	}

	@Override
	public Set<TargetCapability> capabilities() {
		Set<TargetCapability> capabilities = EnumSet.of(TargetCapability.READ, TargetCapability.PREVIEW_ITEM);
		if (this.player().present() && (this.player().creative() || this.backend().isPresent())) {
			capabilities.add(TargetCapability.WRITE);
		}
		return capabilities;
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		if (!this.player().present()) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("no_player")));
		}
		return CompletableFuture.completedFuture(this.player().slot(this.slot).tag().copy());
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		PlayerService player = this.player();
		if (!player.present()) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("no_player")));
		}
		if (player.creative()) {
			GameItem item = this.ctx.client().items().fromTag(tag);
			if (!player.setSlot(this.slot, item)) {
				return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("save.slot_failed")));
			}
			return CompletableFuture.completedFuture(null);
		}
		Optional<WorldBackend> backend = this.backend();
		if (backend.isEmpty()) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("save.not_creative")));
		}
		return backend.get().setPlayerSlot(player.uuid(), this.slot, tag);
	}

	@Override
	public GameItem previewItem(CompoundTag tag) {
		return this.ctx.client().items().fromTag(tag);
	}
}
