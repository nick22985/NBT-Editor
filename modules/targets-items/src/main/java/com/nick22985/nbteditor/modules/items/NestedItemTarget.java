package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.doc.NbtDocument;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.GameItem;
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
public final class NestedItemTarget implements Target {
	private final ModuleContext ctx;
	private final Target parent;
	private final NbtPath path;
	private Text name;

	public NestedItemTarget(ModuleContext ctx, Target parent, NbtPath path) {
		this.ctx = ctx;
		this.parent = parent;
		this.path = path;
	}

	public Target parent() {
		return this.parent;
	}

	public NbtPath path() {
		return this.path;
	}

	@Override
	public String id() {
		return this.parent.id() + "/" + this.path;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ITEM;
	}

	@Override
	public Text displayName() {
		return this.name == null ? this.ctx.tr("nested") : this.name;
	}

	@Override
	public int inventorySlot() {
		return this.parent.inventorySlot();
	}

	@Override
	public Text source() {
		return this.ctx.tr("source.nested", this.parent.displayName(), this.path.toString());
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
	public CompletableFuture<CompoundTag> load() {
		return this.parent.load().thenApply(root -> {
			Tag tag = this.path.get(root);
			if (!(tag instanceof CompoundTag)) {
				throw new TargetException(this.ctx.tr("nested.missing", this.path.toString()));
			}
			CompoundTag item = ((CompoundTag) tag).copy();
			this.name = this.ctx.client().items().fromTag(item).name();
			return item;
		});
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		return this.parent.load().thenCompose(root -> {
			NbtDocument document = new NbtDocument(root);
			document.set(this.path, tag);
			return this.parent.save((CompoundTag) document.root());
		});
	}

	@Override
	public GameItem previewItem(CompoundTag tag) {
		return this.ctx.client().items().fromTag(tag);
	}
}
