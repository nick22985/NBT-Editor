package com.nick22985.nbteditor.container;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.EntityTrait;
import com.nick22985.nbteditor.target.SubjectKind;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ContainerSubject(SubjectKind kind, String id, String entity, Set<EntityTrait> traits, CompoundTag root, GameVersion version) {
	public ContainerSubject {
		traits = Set.copyOf(traits);
	}

	public boolean item() {
		return this.kind == SubjectKind.ITEM;
	}

	public boolean has(EntityTrait trait) {
		return this.traits.contains(trait);
	}

	public ContainerSubject withRoot(CompoundTag root) {
		return new ContainerSubject(this.kind, this.id, this.entity, this.traits, root, this.version);
	}
}
