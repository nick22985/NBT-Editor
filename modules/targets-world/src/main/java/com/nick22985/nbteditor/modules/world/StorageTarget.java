package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StorageTarget extends WorldTarget {
	private final String storageId;

	public StorageTarget(ModuleContext ctx, String storageId) {
		super(ctx);
		this.storageId = storageId;
	}

	@Override
	public String id() {
		return "storage:" + this.storageId;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.STORAGE;
	}

	@Override
	public Text displayName() {
		return Text.literal(this.storageId);
	}

	@Override
	public Text source() {
		return this.ctx.tr("storage");
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		Optional<WorldBackend> backend = this.backend(TargetCapability.READ);
		if (backend.isEmpty()) {
			return this.failure("no_backend");
		}
		return backend.get().readStorage(this.storageId).thenApply(tag -> tag == null ? new CompoundTag() : tag);
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		return this.writer().writeStorage(this.storageId, tag);
	}
}
