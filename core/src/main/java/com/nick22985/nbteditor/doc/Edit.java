package com.nick22985.nbteditor.doc;

import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.Tag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record Edit(EditKind kind, NbtPath path, Tag before, Tag after, int position, String renamedFrom) {
	public static Edit set(NbtPath path, Tag before, Tag after, int position) {
		return new Edit(EditKind.SET, path, before, after, position, null);
	}

	public static Edit insert(NbtPath path, Tag tag) {
		return new Edit(EditKind.INSERT, path, null, tag, -1, null);
	}

	public static Edit remove(NbtPath path, Tag tag, int position) {
		return new Edit(EditKind.REMOVE, path, tag, null, position, null);
	}

	public static Edit rename(NbtPath path, String from) {
		return new Edit(EditKind.RENAME, path, null, null, -1, from);
	}

	public Edit inverse() {
		switch (kind) {
			case SET:
				return new Edit(EditKind.SET, path, after, before, position, null);
			case INSERT:
				return new Edit(EditKind.REMOVE, path, after, null, position, null);
			case REMOVE:
				return new Edit(EditKind.INSERT, path, null, before, position, null);
			case RENAME:
				return new Edit(EditKind.RENAME, path.withLast(renamedFrom), null, null, -1, (String) path.last());
			default:
				throw new IllegalStateException();
		}
	}
}
