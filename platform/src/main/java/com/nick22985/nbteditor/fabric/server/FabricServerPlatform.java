package com.nick22985.nbteditor.fabric.server;

import com.nick22985.nbteditor.fabric.McRegistryAccess;
import com.nick22985.nbteditor.fabric.item.DecodedItem;
import com.nick22985.nbteditor.fabric.util.McBlockUtil;
import com.nick22985.nbteditor.fabric.util.McEntityUtil;
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.fabric.util.McItemUtil;
import com.nick22985.nbteditor.fabric.util.McPermissionUtil;
import com.nick22985.nbteditor.fabric.util.McTagUtil;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.platform.PlayerInfo;
import com.nick22985.nbteditor.platform.ServerPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
//keys
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.11 {
import java.util.TreeMap;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
//?} else
//import net.minecraft.world.level.GameRules;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricServerPlatform implements ServerPlatform {
	private final McRegistryAccess registries;

	public FabricServerPlatform(McRegistryAccess registries) {
		this.registries = registries;
	}

	private MinecraftServer server() {
		return this.registries.server();
	}

	private <T> CompletableFuture<T> onServer(Supplier<T> task) {
		MinecraftServer server = this.server();
		if (server == null) {
			return CompletableFuture.failedFuture(new IllegalStateException("No server running"));
		}
		if (server.isSameThread()) {
			try {
				return CompletableFuture.completedFuture(task.get());
			} catch (RuntimeException e) {
				return CompletableFuture.failedFuture(e);
			}
		}
		return CompletableFuture.supplyAsync(task, server);
	}

	private ServerLevel level(String dimension) {
		MinecraftServer server = this.server();
		Identifier id = dimension == null ? null : McIdUtil.tryParse(dimension);
		if (id == null) {
			return server.overworld();
		}
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
		return level == null ? server.overworld() : level;
	}

	private ServerPlayer player(UUID uuid) {
		MinecraftServer server = this.server();
		return server == null ? null : server.getPlayerList().getPlayer(uuid);
	}

	private Entity entity(String dimension, UUID uuid) {
		Entity entity = this.level(dimension).getEntity(uuid);
		if (entity != null) {
			return entity;
		}
		for (ServerLevel level : this.server().getAllLevels()) {
			entity = level.getEntity(uuid);
			if (entity != null) {
				return entity;
			}
		}
		return null;
	}

	@Override
	public boolean running() {
		MinecraftServer server = this.server();
		return server != null && server.isRunning();
	}

	@Override
	public int permissionLevel(UUID uuid) {
		return McPermissionUtil.level(this.player(uuid));
	}

	@Override
	public List<PlayerInfo> players() {
		MinecraftServer server = this.server();
		List<PlayerInfo> out = new ArrayList<>();
		if (server != null) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				out.add(new PlayerInfo(player.getUUID(), player.getName().getString()));
			}
		}
		return out;
	}

	@Override
	public List<String> dimensions() {
		MinecraftServer server = this.server();
		List<String> out = new ArrayList<>();
		if (server != null) {
			for (ResourceKey<Level> key : server.levelKeys()) {
				out.add(key.identifier().toString());
			}
		}
		return out;
	}

	@Override
	public CompletableFuture<BlockData> readBlock(String dimension, BlockPos pos) {
		return this.onServer(() -> {
			ServerLevel level = this.level(dimension);
			net.minecraft.core.BlockPos at = McBlockUtil.toMc(pos);
			BlockState state = level.getBlockState(at);
			BlockEntity blockEntity = level.getBlockEntity(at);
			CompoundTag tag = blockEntity == null ? null : McTagUtil.fromMc(McBlockUtil.saveBlockEntity(blockEntity));
			return new BlockData(McBlockUtil.id(state), McBlockUtil.properties(state), tag);
		});
	}

	@Override
	public CompletableFuture<Boolean> writeBlock(String dimension, BlockPos pos, BlockData data, BlockWriteFlags flags) {
		return this.onServer(() -> {
			ServerLevel level = this.level(dimension);
			net.minecraft.core.BlockPos at = McBlockUtil.toMc(pos);
			BlockState state = flags.keepState() ? level.getBlockState(at) : McBlockUtil.state(data.blockId(), data.properties());
			if (!flags.keepState()) {
				int update = 0;
				if (flags.updateNeighbors()) {
					update |= Block.UPDATE_NEIGHBORS;
				}
				if (flags.notifyClients()) {
					update |= Block.UPDATE_CLIENTS;
				}
				if (!flags.triggerUpdates()) {
					update |= Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
				}
				level.setBlock(at, state, update);
			}
			if (flags.recreate()) {
				level.removeBlockEntity(at);
			}
			if (data.blockEntity() != null) {
				BlockEntity blockEntity = level.getBlockEntity(at);
				if (blockEntity == null) {
					return data.blockEntity().isEmpty();
				}
				McBlockUtil.loadBlockEntity(blockEntity, McTagUtil.toMc(data.blockEntity()));
				blockEntity.setChanged();
				level.sendBlockUpdated(at, state, state, Block.UPDATE_ALL);
			}
			return true;
		});
	}

	@Override
	public CompletableFuture<CompoundTag> readEntity(String dimension, UUID uuid) {
		return this.onServer(() -> {
			Entity entity = this.entity(dimension, uuid);
			return entity == null ? null : McEntityUtil.saveTree(entity);
		});
	}

	@Override
	public CompletableFuture<Boolean> summonEntity(String dimension, CompoundTag tag) {
		return this.onServer(() -> tag != null && McEntityUtil.spawnTree(this.level(dimension), tag) != null);
	}

	@Override
	public CompletableFuture<Boolean> writeEntity(String dimension, UUID uuid, CompoundTag tag, boolean recreate) {
		return this.onServer(() -> {
			Entity entity = this.entity(dimension, uuid);
			if (entity == null) {
				return false;
			}
			if ((!recreate || entity instanceof Player) && McEntityUtil.sameTree(entity, tag)) {
				McEntityUtil.loadTree(entity, tag);
				return true;
			}
			if (entity instanceof Player || !(McEntityUtil.level(entity) instanceof ServerLevel)) {
				return false;
			}
			ServerLevel level = (ServerLevel) McEntityUtil.level(entity);
			Entity vehicle = entity.getVehicle();
			McEntityUtil.discardTree(entity);
			Entity created = McEntityUtil.spawnTree(level, tag);
			if (created == null) {
				return false;
			}
			if (vehicle != null) {
				float yaw = created.getYRot();
				float pitch = created.getXRot();
				McEntityUtil.ride(created, vehicle);
				McEntityUtil.restoreRotation(created, yaw, pitch);
			}
			return true;
		});
	}

	@Override
	public CompletableFuture<CompoundTag> readPlayer(UUID uuid) {
		return this.onServer(() -> {
			ServerPlayer player = this.player(uuid);
			return player == null ? null : McTagUtil.fromMc(McEntityUtil.save(player));
		});
	}

	@Override
	public CompletableFuture<Boolean> writePlayer(UUID uuid, CompoundTag tag) {
		return this.onServer(() -> {
			ServerPlayer player = this.player(uuid);
			if (player == null) {
				return false;
			}
			McEntityUtil.load(player, McTagUtil.toMc(tag));
			player.inventoryMenu.broadcastChanges();
			player.containerMenu.broadcastChanges();
			return true;
		});
	}

	@Override
	public CompletableFuture<Boolean> setPlayerSlot(UUID uuid, int slot, CompoundTag item) {
		return this.onServer(() -> {
			ServerPlayer player = this.player(uuid);
			if (player == null || slot < 0 || slot >= player.getInventory().getContainerSize()) {
				return false;
			}
			DecodedItem decoded = McItemUtil.decode(item);
			player.getInventory().setItem(slot, decoded.stack());
			player.inventoryMenu.broadcastChanges();
			player.containerMenu.broadcastChanges();
			return true;
		});
	}

	@Override
	public CompletableFuture<List<String>> storageIds() {
		return this.onServer(() -> this.server().getCommandStorage().keys().map(Identifier::toString).sorted().collect(Collectors.toList()));
	}

	@Override
	public CompletableFuture<CompoundTag> readStorage(String id) {
		return this.onServer(() -> {
			Identifier key = McIdUtil.tryParse(id);
			return key == null ? null : McTagUtil.fromMc(this.server().getCommandStorage().get(key));
		});
	}

	@Override
	public CompletableFuture<Boolean> writeStorage(String id, CompoundTag tag) {
		return this.onServer(() -> {
			Identifier key = McIdUtil.tryParse(id);
			if (key == null) {
				return false;
			}
			this.server().getCommandStorage().set(key, McTagUtil.toMc(tag));
			return true;
		});
	}

	private GameRules rules() {
		//? if >=26.1 {
		return this.server().getGameRules();
		//?} else if >=1.21.11 {
		/*return this.server().overworld().getGameRules();
		*///?} else
		//return this.server().getGameRules();
	}

	@Override
	public CompletableFuture<Map<String, String>> gameRules() {
		return this.onServer(() -> {
			GameRules rules = this.rules();
			//? if >=1.21.11 {
			Map<String, String> out = new TreeMap<>();
			rules.availableRules().forEach(rule -> out.put(rule.id(), rules.getAsString(rule)));
			return out;
			//?} else if >=1.21.2 {
			/*GameRuleCollector collector = new GameRuleCollector(rules);
			rules.visitGameRuleTypes(collector);
			return collector.values();
			*///?} else {
			/*GameRuleCollector collector = new GameRuleCollector(rules);
			GameRules.visitGameRuleTypes(collector);
			return collector.values();
			*///?}
		});
	}

	@Override
	public CompletableFuture<Boolean> setGameRule(String rule, String value) {
		return this.onServer(() -> {
			//? if >=1.21.11 {
			GameRules rules = this.rules();
			GameRule<?> match = rules.availableRules().filter(candidate -> candidate.id().equals(rule)).findFirst().orElse(null);
			return match != null && this.applyRule(rules, match, value);
			//?} else {
			/*MinecraftServer server = this.server();
			//? if >=1.19 {
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "gamerule " + rule + " " + value);
			//?} else
			//server.getCommands().performCommand(server.createCommandSourceStack().withSuppressedOutput(), "gamerule " + rule + " " + value);
			return true;
			*///?}
		});
	}
	//? if >=1.21.11 {

	private <T> boolean applyRule(GameRules rules, GameRule<T> rule, String value) {
		T parsed = rule.deserialize(value).result().orElse(null);
		if (parsed == null) {
			return false;
		}
		rules.set(rule, parsed, this.server());
		return true;
	}
	//?}
}
