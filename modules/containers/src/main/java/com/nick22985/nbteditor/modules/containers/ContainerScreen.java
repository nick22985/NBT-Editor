package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
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
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerScreen extends InventoryScreen {
	private final ContainerOpener opener;
	private final Target target;
	private final ContainerFormat format;
	private final ListSection container;
	private ContainerSubject subject;
	private boolean dirty;
	private boolean saving;
	private InventoryStatus status;

	public ContainerScreen(ModuleContext ctx, ContainerOpener opener, Target target, ContainerSubject subject, ContainerFormat format, Text title) {
		super(ctx, title);
		this.opener = opener;
		this.target = target;
		this.subject = subject;
		this.format = format;
		this.container = this.addSection(new ListSection(title, format.layout(subject), this.read(subject), this.items().empty()));
		this.container.onChange(this::markDirty);
		this.container.targets(index -> new ContainerSlotTarget(ctx, opener, target, index));
		this.container.mode(this.defaultMode());
		ItemTargets targets = ctx.services().find(ItemTargets.class).orElse(null);
		PlayerSection player = this.addSection(new PlayerSection(ctx.tr("inventory"), this.player(), targets));
		if (target.inventorySlot() >= 0) {
			player.lockPlayerSlot(target.inventorySlot());
		}
		if (this.container.mode() == SlotMode.READ_ONLY) {
			this.status = new InventoryStatus(ctx.tr("status.read_only"), Theme.FAINT, PixelSprites.LOCK);
		}
	}

	public SubjectKind kind() {
		return this.target.kind();
	}

	private boolean writable() {
		return this.target.can(TargetCapability.WRITE);
	}

	private SlotMode defaultMode() {
		if (this.writable()) {
			return SlotMode.NORMAL;
		}
		return this.player().creative() ? SlotMode.SOURCE : SlotMode.READ_ONLY;
	}

	private List<GameItem> read(ContainerSubject subject) {
		List<GameItem> items = new ArrayList<>();
		for (CompoundTag tag : this.format.read(subject)) {
			items.add(tag.isEmpty() ? this.items().empty() : this.items().fromTag(tag));
		}
		return items;
	}

	private void markDirty() {
		this.dirty = true;
	}

	@Override
	protected void buildSide() {
		EditorService editor = this.ctx.services().find(EditorService.class).orElse(null);
		if (editor != null) {
			this.sideButton(this.ctx.tr("edit_nbt"), () -> this.save().thenRun(() -> this.ctx.platform().scheduler().client(() -> editor.open(this.target))));
		}
		if (this.writable() && this.player().creative()) {
			boolean copying = this.container.mode() == SlotMode.SOURCE;
			this.sideButton(this.ctx.tr(copying ? "mode.copy" : "mode.normal"), this::toggleMode).tooltip(this.ctx.tr("mode.hint"));
		}
		this.sideButton(this.ctx.tr("close"), this::close);
	}

	private void toggleMode() {
		this.container.mode(this.container.mode() == SlotMode.SOURCE ? SlotMode.NORMAL : SlotMode.SOURCE);
		this.rebuild();
	}

	@Override
	protected Text sideText() {
		Text source = this.target.source();
		return source == null || source.isEmpty() ? null : source;
	}

	@Override
	protected InventoryStatus status() {
		return this.status;
	}

	@Override
	protected void onTick() {
		if (this.dirty && !this.saving) {
			this.save();
		}
	}

	private CompletableFuture<Void> save() {
		if (!this.dirty || !this.writable()) {
			return CompletableFuture.completedFuture(null);
		}
		this.dirty = false;
		this.saving = true;
		this.status = InventoryStatus.of(this.ctx.tr("status.saving"), Theme.YELLOW);
		List<CompoundTag> tags = new ArrayList<>();
		for (GameItem item : this.container.items()) {
			tags.add(InventoryUtil.present(item) ? item.tag().copy() : new CompoundTag());
		}
		ContainerSubject next = this.subject.withRoot(this.subject.root().copy());
		this.format.write(next, tags);
		this.subject = next;
		return this.target.save(next.root().copy()).whenComplete((ignored, error) -> this.ctx.platform().scheduler().client(() -> {
			this.saving = false;
			if (error == null) {
				this.status = new InventoryStatus(this.ctx.tr("status.saved"), Theme.GREEN, PixelSprites.CHECK);
				return;
			}
			this.ctx.log().warn("Could not save container " + this.target.id(), ErrorUtil.unwrap(error));
			this.status = new InventoryStatus(this.ctx.tr("status.failed"), Theme.RED, PixelSprites.CROSS);
			this.ctx.client().screens().toast(this.ctx.tr("save.failed"), ErrorUtil.message(error));
		}));
	}

	@Override
	public void onShown() {
		if (this.dirty || this.saving) {
			return;
		}
		this.target.load().whenComplete((tag, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null || this.dirty || this.saving) {
				return;
			}
			this.subject = this.opener.subject(this.target.kind(), tag, this.opener.version(this.target));
			if (this.format.supports(this.subject)) {
				this.container.replace(this.read(this.subject));
			}
		}));
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.plain(Key.E) && this.topOverlay() == null) {
			this.close();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		this.save();
	}
}
