package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Target {
	String id();

	SubjectKind kind();

	Text displayName();

	Set<TargetCapability> capabilities();

	default boolean can(TargetCapability capability) {
		return this.capabilities().contains(capability);
	}

	CompletableFuture<CompoundTag> load();

	CompletableFuture<Void> save(CompoundTag tag);

	default GameItem previewItem(CompoundTag tag) {
		return null;
	}

	default GameVersion dataVersion() {
		return null;
	}

	default int inventorySlot() {
		return -1;
	}

	default Text source() {
		return Text.EMPTY;
	}

	default NbtPath dataPath(GameVersion version) {
		return this.kind() == SubjectKind.ITEM ? NbtPath.of(ItemStackUtil.dataKey(version)) : NbtPath.ROOT;
	}
}
