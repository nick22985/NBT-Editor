package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.GameVersion;
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
public final class ContainerSlotTarget implements Target {
	private final ModuleContext ctx;
	private final ContainerOpener opener;
	private final Target parent;
	private final int index;
	private Text name;

	public ContainerSlotTarget(ModuleContext ctx, ContainerOpener opener, Target parent, int index) {
		this.ctx = ctx;
		this.opener = opener;
		this.parent = parent;
		this.index = index;
	}

	public Target parent() {
		return this.parent;
	}

	public int index() {
		return this.index;
	}

	@Override
	public String id() {
		return this.parent.id() + "/slot:" + this.index;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ITEM;
	}

	@Override
	public Text displayName() {
		return this.name == null ? this.ctx.tr("slot", this.index + 1) : this.name;
	}

	@Override
	public Text source() {
		return this.ctx.tr("slot.source", this.parent.displayName(), this.index + 1);
	}

	@Override
	public Set<TargetCapability> capabilities() {
		Set<TargetCapability> capabilities = EnumSet.of(TargetCapability.READ, TargetCapability.PREVIEW_ITEM);
		if (this.parent.can(TargetCapability.WRITE)) {
			capabilities.add(TargetCapability.WRITE);
		}
		return capabilities;
	}

	@Override
	public GameVersion dataVersion() {
		return this.parent.dataVersion();
	}

	@Override
	public int inventorySlot() {
		return this.parent.inventorySlot();
	}

	private ContainerSubject subject(CompoundTag root) {
		return this.opener.subject(this.parent.kind(), root, this.opener.version(this.parent));
	}

	private ContainerFormat format(ContainerSubject subject) {
		return this.opener.format(subject).orElseThrow(() -> new TargetException(this.ctx.tr("unsupported")));
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		return this.parent.load().thenApply(root -> {
			ContainerSubject subject = this.subject(root);
			List<CompoundTag> items = this.format(subject).read(subject);
			if (this.index >= items.size()) {
				throw new TargetException(this.ctx.tr("slot.missing", this.index + 1));
			}
			CompoundTag item = items.get(this.index);
			this.name = this.ctx.client().items().fromTag(item).name();
			return item;
		});
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		return this.parent.load().thenCompose(root -> {
			ContainerSubject subject = this.subject(root);
			ContainerFormat format = this.format(subject);
			List<CompoundTag> items = new ArrayList<>(format.read(subject));
			if (this.index >= items.size()) {
				throw new TargetException(this.ctx.tr("slot.missing", this.index + 1));
			}
			items.set(this.index, tag.copy());
			format.write(subject, items);
			return this.parent.save(root);
		});
	}

	@Override
	public GameItem previewItem(CompoundTag tag) {
		return this.ctx.client().items().fromTag(tag);
	}
}
