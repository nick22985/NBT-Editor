package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.platform.WorldService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockDocUtil;
import com.nick22985.nbteditor.util.BlockStateUtil;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockTarget extends WorldTarget {
	private final String dimension;
	private final BlockPos pos;
	private final Setting<Boolean> blockUpdates;
	private final Setting<Boolean> recreate;
	private String blockId;

	public BlockTarget(ModuleContext ctx, String dimension, BlockPos pos, Setting<Boolean> blockUpdates, Setting<Boolean> recreate) {
		super(ctx);
		this.dimension = dimension;
		this.pos = pos;
		this.blockUpdates = blockUpdates;
		this.recreate = recreate;
	}

	public String dimension() {
		return this.dimension;
	}

	public BlockPos pos() {
		return this.pos;
	}

	private WorldService world() {
		return this.ctx.client().world();
	}

	private boolean here() {
		WorldService world = this.world();
		return world.present() && (this.dimension == null || Objects.equals(this.dimension, world.dimension()));
	}

	@Override
	public String id() {
		return "block:" + this.dimension + ":" + this.pos.command();
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.BLOCK;
	}

	@Override
	public Text displayName() {
		String id = this.blockId;
		if (id == null && this.here()) {
			BlockData data = this.world().clientBlock(this.pos);
			id = data == null ? null : data.blockId();
		}
		return id == null ? this.ctx.tr("block") : this.ctx.client().registries().name(RegistryService.BLOCK, id);
	}

	@Override
	public Text source() {
		return this.ctx.tr("block.source", this.pos.x(), this.pos.y(), this.pos.z(), this.dimension == null ? "" : this.dimension);
	}

	@Override
	protected boolean clientReadable() {
		return this.here();
	}

	@Override
	public NbtPath dataPath(GameVersion version) {
		return NbtPath.of(BlockDocUtil.NBT);
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		Optional<WorldBackend> backend = this.backend(TargetCapability.READ);
		if (backend.isPresent()) {
			return backend.get().readBlock(this.dimension, this.pos).thenApply(this::loaded);
		}
		if (!this.here()) {
			return this.failure("not_here");
		}
		BlockData data = this.world().clientBlock(this.pos);
		if (data == null) {
			return this.failure("block.unloaded");
		}
		return CompletableFuture.completedFuture(this.loaded(data));
	}

	private CompoundTag loaded(BlockData data) {
		if (data == null) {
			throw new TargetException(this.ctx.tr("block.unloaded"));
		}
		this.blockId = data.blockId();
		return BlockDocUtil.toTag(data);
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		BlockData data = BlockStateUtil.sync(BlockDocUtil.fromTag(tag));
		if (data.blockEntity() != null && data.blockEntity().isEmpty()) {
			data = new BlockData(data.blockId(), data.properties(), null);
		}
		this.blockId = data.blockId();
		boolean updates = this.blockUpdates.get();
		BlockWriteFlags flags = new BlockWriteFlags(updates, true, false, updates, this.recreate.get());
		return this.writer().writeBlock(this.dimension, this.pos, data, flags);
	}
}
