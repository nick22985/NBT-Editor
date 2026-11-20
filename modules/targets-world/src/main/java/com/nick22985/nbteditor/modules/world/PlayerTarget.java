package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PlayerTarget extends WorldTarget {
	private final UUID uuid;
	private final String name;

	public PlayerTarget(ModuleContext ctx, UUID uuid, String name) {
		super(ctx);
		this.uuid = uuid;
		this.name = name;
	}

	private PlayerService player() {
		return this.ctx.client().player();
	}

	private boolean self() {
		PlayerService player = this.player();
		return this.uuid == null || player.present() && this.uuid.equals(player.uuid());
	}

	private UUID resolved() {
		return this.uuid == null ? this.player().uuid() : this.uuid;
	}

	@Override
	public String id() {
		return this.uuid == null ? "player:self" : "player:" + this.uuid;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.PLAYER;
	}

	@Override
	public Text displayName() {
		if (this.name != null) {
			return Text.literal(this.name);
		}
		return Text.literal(this.player().name());
	}

	@Override
	public Text source() {
		return this.self() ? this.ctx.tr("player.self") : this.ctx.tr("player.other");
	}

	@Override
	protected boolean clientReadable() {
		return this.self() && this.player().present();
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		if (!this.player().present()) {
			return this.failure("no_world");
		}
		Optional<WorldBackend> backend = this.backend(TargetCapability.READ);
		if (backend.isPresent()) {
			return backend.get().readPlayer(this.resolved()).thenApply(this::loaded);
		}
		if (!this.self()) {
			return this.failure("no_backend");
		}
		return CompletableFuture.completedFuture(this.loaded(this.ctx.client().world().clientEntity(this.player().networkId())));
	}

	private CompoundTag loaded(CompoundTag tag) {
		if (tag == null) {
			throw new TargetException(this.ctx.tr("player.missing"));
		}
		return tag;
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		return this.writer().writePlayer(this.resolved(), tag);
	}
}
