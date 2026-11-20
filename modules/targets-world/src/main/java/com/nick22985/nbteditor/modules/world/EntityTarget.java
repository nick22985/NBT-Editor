package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.platform.WorldService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EntityTarget extends WorldTarget {
	private final String dimension;
	private final UUID uuid;
	private final int networkId;
	private final Setting<Boolean> recreate;
	private String typeId;

	public EntityTarget(ModuleContext ctx, String dimension, UUID uuid, int networkId, String typeId, Setting<Boolean> recreate) {
		super(ctx);
		this.dimension = dimension;
		this.uuid = uuid;
		this.networkId = networkId;
		this.typeId = typeId;
		this.recreate = recreate;
	}

	public UUID uuid() {
		return this.uuid;
	}

	private WorldService world() {
		return this.ctx.client().world();
	}

	private boolean here() {
		WorldService world = this.world();
		return world.present() && this.networkId >= 0 && (this.dimension == null || Objects.equals(this.dimension, world.dimension()));
	}

	@Override
	public String id() {
		return "entity:" + this.uuid;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ENTITY;
	}

	@Override
	public Text displayName() {
		return this.typeId == null ? this.ctx.tr("entity") : this.ctx.client().registries().name(RegistryService.ENTITY_TYPE, this.typeId);
	}

	@Override
	public Text source() {
		return Text.literal(this.uuid.toString());
	}

	@Override
	protected boolean clientReadable() {
		return this.here();
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		Optional<WorldBackend> backend = this.backend(TargetCapability.READ);
		if (backend.isPresent()) {
			return backend.get().readEntity(this.dimension, this.uuid).thenApply(this::loaded);
		}
		if (!this.here()) {
			return this.failure("not_here");
		}
		return CompletableFuture.completedFuture(this.loaded(this.world().clientEntity(this.networkId)));
	}

	private CompoundTag loaded(CompoundTag tag) {
		if (tag == null) {
			throw new TargetException(this.ctx.tr("entity.missing"));
		}
		String id = tag.getString("id");
		if (id != null) {
			this.typeId = id;
		} else if (this.typeId != null) {
			tag.putAt(0, "id", StringTag.of(this.typeId));
		}
		return tag;
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		return this.writer().writeEntity(this.dimension, this.uuid, tag, this.recreate.get());
	}
}
