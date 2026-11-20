package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockDocUtil;
import com.nick22985.nbteditor.util.TagMapUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ServerBridgeBackend implements WorldBackend {
	private static final int TIMEOUT_TICKS = 200;
	private static final int RETRY_TICKS = 100;

	private final ModuleContext ctx;
	private final Setting<Boolean> mismatchWarning;
	private final AtomicInteger ids = new AtomicInteger();
	private final Map<Integer, CompletableFuture<Tag>> pending = new ConcurrentHashMap<>();
	private volatile BridgeWelcome welcome;
	private boolean greeted;
	private int ticks;

	public ServerBridgeBackend(ModuleContext ctx, Setting<Boolean> mismatchWarning) {
		this.ctx = ctx;
		this.mismatchWarning = mismatchWarning;
	}

	public void register() {
		this.ctx.net().registerClient(BridgePackets.WELCOME, message -> this.ctx.platform().scheduler().client(() -> this.welcomed(message)));
		this.ctx.net().registerClient(BridgePackets.RESPONSE, message -> this.ctx.platform().scheduler().client(() -> this.respond(message)));
		this.ctx.hooks().on(CoreHooks.CLIENT_TICK, event -> this.tick(event.inWorld()));
		this.ctx.hooks().on(CoreHooks.WORLD_JOIN, event -> this.reset());
		this.ctx.hooks().on(CoreHooks.WORLD_LEAVE, event -> this.reset());
	}

	public BridgeWelcome welcome() {
		return this.welcome;
	}

	private void tick(boolean inWorld) {
		this.ticks++;
		if (!inWorld || !this.ctx.net().serverAvailable()) {
			return;
		}
		BridgeWelcome current = this.welcome;
		boolean retry = current != null && !current.allowed() && current.protocol() == BridgePackets.PROTOCOL && this.ticks % RETRY_TICKS == 0;
		if (!this.greeted || retry) {
			this.greeted = true;
			this.ctx.net().sendToServer(BridgePackets.HELLO, BridgePackets.PROTOCOL);
		}
	}

	private void welcomed(BridgeWelcome message) {
		this.welcome = message;
		if (message.protocol() != BridgePackets.PROTOCOL) {
			this.ctx.log().warn("Server bridge protocol " + message.protocol() + " does not match " + BridgePackets.PROTOCOL);
			if (this.mismatchWarning.get()) {
				this.ctx.client().screens().toast(this.ctx.tr("mismatch.title"), this.ctx.tr(message.protocol() > BridgePackets.PROTOCOL ? "mismatch.client_old" : "mismatch.server_old"));
			}
		}
	}

	private void reset() {
		this.welcome = null;
		this.greeted = false;
		List<CompletableFuture<Tag>> waiting = new ArrayList<>(this.pending.values());
		this.pending.clear();
		for (CompletableFuture<Tag> future : waiting) {
			future.completeExceptionally(this.error("disconnected"));
		}
	}

	private void respond(BridgeResponse response) {
		CompletableFuture<Tag> future = this.pending.remove(response.id());
		if (future == null) {
			return;
		}
		if (response.succeeded()) {
			future.complete(response.data());
		} else {
			if ("denied".equals(response.error())) {
				this.welcome = null;
				this.greeted = false;
			}
			future.completeExceptionally(this.error(response.error()));
		}
	}

	private TargetException error(String key) {
		return new TargetException(this.ctx.tr("error." + key));
	}

	private CompletableFuture<Tag> request(BridgeOp op, IntFunction<BridgeRequest> builder) {
		if (!this.available()) {
			return CompletableFuture.failedFuture(this.error("unavailable"));
		}
		int id = this.ids.incrementAndGet();
		CompletableFuture<Tag> future = new CompletableFuture<>();
		this.pending.put(id, future);
		this.ctx.net().sendToServer(BridgePackets.REQUEST, builder.apply(id));
		this.ctx.platform().scheduler().later(TIMEOUT_TICKS, () -> {
			CompletableFuture<Tag> late = this.pending.remove(id);
			if (late != null) {
				late.completeExceptionally(this.error("timeout"));
			}
		});
		return future;
	}

	private CompletableFuture<CompoundTag> compound(CompletableFuture<Tag> result) {
		return result.thenApply(tag -> tag instanceof CompoundTag ? (CompoundTag) tag : null);
	}

	@Override
	public String id() {
		return "server";
	}

	@Override
	public int priority() {
		return 100;
	}

	@Override
	public boolean available() {
		BridgeWelcome current = this.welcome;
		return current != null && current.allowed() && current.protocol() == BridgePackets.PROTOCOL && this.ctx.net().serverAvailable();
	}

	@Override
	public Text name() {
		return this.ctx.tr("name");
	}

	@Override
	public boolean supports(SubjectKind kind, TargetCapability capability) {
		return kind != SubjectKind.ITEM && kind != SubjectKind.FILE;
	}

	@Override
	public CompletableFuture<BlockData> readBlock(String dimension, BlockPos pos) {
		return this.compound(this.request(BridgeOp.READ_BLOCK, id -> BridgeRequest.of(id, BridgeOp.READ_BLOCK).at(dimension, pos))).thenApply(tag -> tag == null ? null : BlockDocUtil.fromTag(tag));
	}

	@Override
	public CompletableFuture<Void> writeBlock(String dimension, BlockPos pos, BlockData data, BlockWriteFlags flags) {
		return this.request(BridgeOp.WRITE_BLOCK, id -> BridgeRequest.of(id, BridgeOp.WRITE_BLOCK).at(dimension, pos).data(BlockDocUtil.toTag(data)).flags(flags)).thenApply(tag -> null);
	}

	@Override
	public CompletableFuture<CompoundTag> readEntity(String dimension, UUID uuid) {
		return this.compound(this.request(BridgeOp.READ_ENTITY, id -> BridgeRequest.of(id, BridgeOp.READ_ENTITY).in(dimension).subject(uuid)));
	}

	@Override
	public CompletableFuture<Void> writeEntity(String dimension, UUID uuid, CompoundTag tag, boolean recreate) {
		BlockWriteFlags flags = new BlockWriteFlags(false, false, false, false, recreate);
		return this.request(BridgeOp.WRITE_ENTITY, id -> BridgeRequest.of(id, BridgeOp.WRITE_ENTITY).in(dimension).subject(uuid).data(tag).flags(flags)).thenApply(result -> null);
	}

	@Override
	public CompletableFuture<Void> summonEntity(String dimension, CompoundTag tag) {
		return this.request(BridgeOp.SUMMON_ENTITY, id -> BridgeRequest.of(id, BridgeOp.SUMMON_ENTITY).in(dimension).data(tag)).thenApply(result -> null);
	}

	@Override
	public CompletableFuture<CompoundTag> readPlayer(UUID uuid) {
		return this.compound(this.request(BridgeOp.READ_PLAYER, id -> BridgeRequest.of(id, BridgeOp.READ_PLAYER).subject(uuid)));
	}

	@Override
	public CompletableFuture<Void> writePlayer(UUID uuid, CompoundTag tag) {
		return this.request(BridgeOp.WRITE_PLAYER, id -> BridgeRequest.of(id, BridgeOp.WRITE_PLAYER).subject(uuid).data(tag)).thenApply(result -> null);
	}

	@Override
	public CompletableFuture<Void> setPlayerSlot(UUID uuid, int slot, CompoundTag item) {
		return this.request(BridgeOp.SET_SLOT, id -> BridgeRequest.of(id, BridgeOp.SET_SLOT).subject(uuid).slot(slot).data(item)).thenApply(result -> null);
	}

	@Override
	public CompletableFuture<List<String>> storageIds() {
		return this.request(BridgeOp.STORAGE_IDS, id -> BridgeRequest.of(id, BridgeOp.STORAGE_IDS)).thenApply(tag -> tag instanceof ListTag ? TagMapUtil.strings((ListTag) tag) : List.of());
	}

	@Override
	public CompletableFuture<CompoundTag> readStorage(String storage) {
		return this.compound(this.request(BridgeOp.READ_STORAGE, id -> BridgeRequest.of(id, BridgeOp.READ_STORAGE).entry(storage, null)));
	}

	@Override
	public CompletableFuture<Void> writeStorage(String storage, CompoundTag tag) {
		return this.request(BridgeOp.WRITE_STORAGE, id -> BridgeRequest.of(id, BridgeOp.WRITE_STORAGE).entry(storage, null).data(tag)).thenApply(result -> null);
	}

	@Override
	public CompletableFuture<Map<String, String>> gameRules() {
		return this.compound(this.request(BridgeOp.GAME_RULES, id -> BridgeRequest.of(id, BridgeOp.GAME_RULES))).thenApply(tag -> tag == null ? Map.of() : TagMapUtil.toStrings(tag));
	}

	@Override
	public CompletableFuture<Void> setGameRule(String rule, String value) {
		return this.request(BridgeOp.SET_GAME_RULE, id -> BridgeRequest.of(id, BridgeOp.SET_GAME_RULE).entry(rule, value)).thenApply(result -> null);
	}
}
