package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.platform.ServerPlatform;
import com.nick22985.nbteditor.util.BlockDocUtil;
import com.nick22985.nbteditor.util.TagMapUtil;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BridgeServer {
	private static final Pattern STORAGE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
	private static final Pattern GAME_RULE = Pattern.compile("[A-Za-z0-9_.:-]+");
	private static final int MAX_SLOT = 255;

	private final ModuleContext ctx;
	private final Setting<Integer> permission;
	private final Setting<Boolean> otherPlayers;

	public BridgeServer(ModuleContext ctx, Setting<Integer> permission, Setting<Boolean> otherPlayers) {
		this.ctx = ctx;
		this.permission = permission;
		this.otherPlayers = otherPlayers;
	}

	public void register() {
		this.ctx.net().registerServer(BridgePackets.HELLO, (sender, protocol) -> this.ctx.platform().scheduler().server(() -> this.hello(sender)));
		this.ctx.net().registerServer(BridgePackets.REQUEST, (sender, request) -> this.ctx.platform().scheduler().server(() -> this.handle(sender, request)));
	}

	private ServerPlatform server() {
		return this.ctx.server();
	}

	private void hello(UUID sender) {
		ServerPlatform server = this.server();
		if (server == null || !server.running()) {
			return;
		}
		BridgeWelcome welcome = new BridgeWelcome(BridgePackets.PROTOCOL, server.permissionLevel(sender), this.permission.get(), this.otherPlayers.get());
		this.ctx.net().sendToClient(sender, BridgePackets.WELCOME, welcome);
	}

	private void handle(UUID sender, BridgeRequest request) {
		ServerPlatform server = this.server();
		if (server == null || !server.running()) {
			return;
		}
		if (server.permissionLevel(sender) < this.permission.get()) {
			this.reply(sender, BridgeResponse.failed(request.id(), "denied"));
			return;
		}
		String invalid = this.validate(sender, request);
		if (invalid != null) {
			this.reply(sender, BridgeResponse.failed(request.id(), invalid));
			return;
		}
		CompletableFuture<Tag> result;
		try {
			result = this.run(server, sender, request);
		} catch (RuntimeException e) {
			this.ctx.log().warn("Bridge request " + request.op() + " failed", e);
			this.reply(sender, BridgeResponse.failed(request.id(), "failed"));
			return;
		}
		result.whenComplete((data, error) -> {
			if (error != null) {
				this.ctx.log().warn("Bridge request " + request.op() + " failed", error);
				this.reply(sender, BridgeResponse.failed(request.id(), "failed"));
			} else {
				this.reply(sender, BridgeResponse.ok(request.id(), data));
			}
		});
	}

	private void reply(UUID sender, BridgeResponse response) {
		if (this.ctx.net().clientAvailable(sender)) {
			this.ctx.net().sendToClient(sender, BridgePackets.RESPONSE, response);
		}
	}

	private String validate(UUID sender, BridgeRequest request) {
		switch (request.op().kind()) {
			case BLOCK:
				if (request.pos() == null || !this.server().dimensions().contains(request.dimension())) {
					return "invalid";
				}
				break;
			case ENTITY:
				if (request.uuid() == null && request.op() != BridgeOp.SUMMON_ENTITY || !this.server().dimensions().contains(request.dimension())) {
					return "invalid";
				}
				break;
			case PLAYER:
				if (request.uuid() == null) {
					return "invalid";
				}
				if (!request.uuid().equals(sender) && !this.otherPlayers.get()) {
					return "other_players";
				}
				if (request.op() == BridgeOp.SET_SLOT && (request.slot() < 0 || request.slot() > MAX_SLOT)) {
					return "invalid";
				}
				break;
			case STORAGE:
				if (request.op() != BridgeOp.STORAGE_IDS && (request.key() == null || !STORAGE_ID.matcher(request.key()).matches())) {
					return "invalid";
				}
				break;
			case WORLD:
				if (request.op() == BridgeOp.SET_GAME_RULE && (request.key() == null || request.value() == null || !GAME_RULE.matcher(request.key()).matches())) {
					return "invalid";
				}
				break;
			default:
				return "invalid";
		}
		if (request.op().write() && request.op() != BridgeOp.SET_GAME_RULE && !(request.data() instanceof CompoundTag)) {
			return "invalid";
		}
		return null;
	}

	private CompletableFuture<Tag> run(ServerPlatform server, UUID sender, BridgeRequest request) {
		CompoundTag data = request.data() instanceof CompoundTag ? (CompoundTag) request.data() : null;
		switch (request.op()) {
			case READ_BLOCK:
				return server.readBlock(request.dimension(), request.pos()).thenApply(block -> block == null ? null : BlockDocUtil.toTag(block));
			case WRITE_BLOCK:
				BlockWriteFlags flags = request.flags() == null ? BlockWriteFlags.DEFAULT : request.flags();
				return this.written(server.writeBlock(request.dimension(), request.pos(), BlockDocUtil.fromTag(data), flags));
			case READ_ENTITY:
				return server.readEntity(request.dimension(), request.uuid()).thenApply(tag -> tag);
			case WRITE_ENTITY:
				return this.written(server.writeEntity(request.dimension(), request.uuid(), data, request.flags() != null && request.flags().recreate()));
			case READ_PLAYER:
				return server.readPlayer(request.uuid()).thenApply(tag -> tag);
			case WRITE_PLAYER:
				return this.written(server.writePlayer(request.uuid(), data));
			case SET_SLOT:
				return this.written(server.setPlayerSlot(request.uuid(), request.slot(), data));
			case STORAGE_IDS:
				return server.storageIds().thenApply(ids -> ListTag.ofStrings(ids));
			case READ_STORAGE:
				return server.readStorage(request.key()).thenApply(tag -> tag);
			case WRITE_STORAGE:
				return this.written(server.writeStorage(request.key(), data));
			case GAME_RULES:
				return server.gameRules().thenApply(rules -> TagMapUtil.fromStrings(rules));
			case SET_GAME_RULE:
				return this.written(server.setGameRule(request.key(), request.value()));
			case SUMMON_ENTITY:
				return this.written(server.summonEntity(request.dimension(), data));
			default:
				return CompletableFuture.failedFuture(new IllegalArgumentException(request.op().name()));
		}
	}

	private CompletableFuture<Tag> written(CompletableFuture<Boolean> result) {
		return result.thenCompose(ok -> ok ? CompletableFuture.completedFuture(null) : CompletableFuture.failedFuture(new IllegalStateException("rejected")));
	}
}
