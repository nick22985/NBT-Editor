package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.text.Text;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CursorItemTarget implements Target {
	private final ModuleContext ctx;

	public CursorItemTarget(ModuleContext ctx) {
		this.ctx = ctx;
	}

	private PlayerService player() {
		return this.ctx.client().player();
	}

	@Override
	public String id() {
		return "item:cursor";
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ITEM;
	}

	@Override
	public Text displayName() {
		GameItem item = this.player().present() ? this.player().cursor() : null;
		return item == null || item.isEmpty() ? this.ctx.tr("empty_slot") : item.name();
	}

	@Override
	public Text source() {
		return this.ctx.tr("slot.cursor");
	}

	@Override
	public Set<TargetCapability> capabilities() {
		Set<TargetCapability> capabilities = EnumSet.of(TargetCapability.READ, TargetCapability.PREVIEW_ITEM);
		if (this.player().present() && this.player().creative()) {
			capabilities.add(TargetCapability.WRITE);
		}
		return capabilities;
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		if (!this.player().present()) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("no_player")));
		}
		return CompletableFuture.completedFuture(this.player().cursor().tag().copy());
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		if (!this.player().present() || !this.player().creative()) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("save.not_creative")));
		}
		this.player().setCursor(this.ctx.client().items().fromTag(tag));
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public GameItem previewItem(CompoundTag tag) {
		return this.ctx.client().items().fromTag(tag);
	}
}
