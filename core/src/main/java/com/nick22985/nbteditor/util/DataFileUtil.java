package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NamedTag;
import com.nick22985.nbteditor.nbt.NbtCompression;
import com.nick22985.nbteditor.nbt.Tag;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DataFileUtil {
	private DataFileUtil() {
	}

	public static CompoundTag read(Path path) throws IOException {
		if (!Files.isRegularFile(path)) {
			return null;
		}
		Tag tag = NbtIoUtil.readAuto(Files.readAllBytes(path)).tag();
		return tag instanceof CompoundTag ? (CompoundTag) tag : null;
	}

	public static void write(Path path, CompoundTag tag) throws IOException {
		writeAtomic(path, NbtIoUtil.write(new NamedTag("", tag), NbtCompression.GZIP));
	}

	public static void writeAtomic(Path path, byte[] data) throws IOException {
		Path parent = path.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		Files.write(temp, data);
		try {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static Path dataDir(Path gameDir) {
		return gameDir.resolve("nbtedit");
	}
}
