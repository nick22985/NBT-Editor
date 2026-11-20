package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.text.Text;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface WorldBackend {
	String id();

	int priority();

	boolean available();

	Text name();

	default boolean supports(SubjectKind kind, TargetCapability capability) {
		return true;
	}

	CompletableFuture<BlockData> readBlock(String dimension, BlockPos pos);

	CompletableFuture<Void> writeBlock(String dimension, BlockPos pos, BlockData data, BlockWriteFlags flags);

	CompletableFuture<CompoundTag> readEntity(String dimension, UUID uuid);

	CompletableFuture<Void> writeEntity(String dimension, UUID uuid, CompoundTag tag, boolean recreate);

	CompletableFuture<Void> summonEntity(String dimension, CompoundTag tag);

	CompletableFuture<CompoundTag> readPlayer(UUID uuid);

	CompletableFuture<Void> writePlayer(UUID uuid, CompoundTag tag);

	CompletableFuture<Void> setPlayerSlot(UUID uuid, int slot, CompoundTag item);

	CompletableFuture<List<String>> storageIds();

	CompletableFuture<CompoundTag> readStorage(String id);

	CompletableFuture<Void> writeStorage(String id, CompoundTag tag);

	CompletableFuture<Map<String, String>> gameRules();

	CompletableFuture<Void> setGameRule(String rule, String value);
}
