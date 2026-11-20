package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.BlockHit;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.EntityHit;
import com.nick22985.nbteditor.platform.Hit;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.WorldTargets;
import java.util.Optional;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class WorldTargetFactory implements WorldTargets {
	private static final String PLAYER_TYPE = "minecraft:player";

	private final ModuleContext ctx;
	private final Setting<Boolean> blockUpdates;
	private final Setting<Boolean> recreate;
	private final Setting<Boolean> otherPlayers;

	public WorldTargetFactory(ModuleContext ctx, Setting<Boolean> blockUpdates, Setting<Boolean> recreate, Setting<Boolean> otherPlayers) {
		this.ctx = ctx;
		this.blockUpdates = blockUpdates;
		this.recreate = recreate;
		this.otherPlayers = otherPlayers;
	}

	@Override
	public Target block(String dimension, BlockPos pos) {
		return new BlockTarget(this.ctx, dimension, pos, this.blockUpdates, this.recreate);
	}

	@Override
	public Target entity(String dimension, UUID uuid, int networkId) {
		return new EntityTarget(this.ctx, dimension, uuid, networkId, null, this.recreate);
	}

	@Override
	public Target self() {
		return new PlayerTarget(this.ctx, null, null);
	}

	@Override
	public Target player(UUID uuid, String name) {
		return new PlayerTarget(this.ctx, uuid, name);
	}

	@Override
	public Target storage(String id) {
		return new StorageTarget(this.ctx, id);
	}

	@Override
	public Target gameRules() {
		return new GameRulesTarget(this.ctx);
	}

	@Override
	public Optional<Target> hit(Hit hit) {
		if (hit instanceof BlockHit) {
			BlockHit block = (BlockHit) hit;
			return Optional.of(this.block(block.dimension(), block.pos()));
		}
		if (hit instanceof EntityHit) {
			EntityHit entity = (EntityHit) hit;
			if (PLAYER_TYPE.equals(entity.typeId())) {
				PlayerService player = this.ctx.client().player();
				if (player.present() && entity.uuid().equals(player.uuid())) {
					return Optional.of(this.self());
				}
				return this.otherPlayers.get() ? Optional.of(this.player(entity.uuid(), entity.name().plain())) : Optional.empty();
			}
			return Optional.of(new EntityTarget(this.ctx, entity.dimension(), entity.uuid(), entity.networkId(), entity.typeId(), this.recreate));
		}
		return Optional.empty();
	}
}
