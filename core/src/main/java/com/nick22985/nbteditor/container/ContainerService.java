package com.nick22985.nbteditor.container;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetOpener;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ContainerService extends TargetOpener {
	ContainerSubject subject(SubjectKind kind, CompoundTag root, GameVersion version);

	Optional<ContainerFormat> format(ContainerSubject subject);

	default Optional<ContainerFormat> format(SubjectKind kind, CompoundTag root, GameVersion version) {
		return this.format(this.subject(kind, root, version));
	}
}
