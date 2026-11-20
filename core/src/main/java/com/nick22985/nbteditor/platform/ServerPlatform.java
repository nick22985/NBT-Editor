package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ServerPlatform {
	boolean running();

	int permissionLevel(UUID player);

	List<PlayerInfo> players();

	List<String> dimensions();

	CompletableFuture<BlockData> readBlock(String dimension, BlockPos pos);

	CompletableFuture<Boolean> writeBlock(String dimension, BlockPos pos, BlockData data, BlockWriteFlags flags);

	CompletableFuture<CompoundTag> readEntity(String dimension, UUID uuid);

	CompletableFuture<Boolean> writeEntity(String dimension, UUID uuid, CompoundTag tag, boolean recreate);

	CompletableFuture<Boolean> summonEntity(String dimension, CompoundTag tag);

	CompletableFuture<CompoundTag> readPlayer(UUID uuid);

	CompletableFuture<Boolean> writePlayer(UUID uuid, CompoundTag tag);

	CompletableFuture<Boolean> setPlayerSlot(UUID uuid, int slot, CompoundTag item);

	CompletableFuture<List<String>> storageIds();

	CompletableFuture<CompoundTag> readStorage(String id);

	CompletableFuture<Boolean> writeStorage(String id, CompoundTag tag);

	CompletableFuture<Map<String, String>> gameRules();

	CompletableFuture<Boolean> setGameRule(String rule, String value);
}
