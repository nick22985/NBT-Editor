package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.nbt.NamedTag;
import com.nick22985.nbteditor.util.NbtIoUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McTagUtil {
	private static final String WRAP = "v";

	private McTagUtil() {
	}

	public static CompoundTag toMc(com.nick22985.nbteditor.nbt.CompoundTag tag) {
		if (tag == null) {
			return null;
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			NbtIoUtil.writeNamed(new NamedTag("", tag), new DataOutputStream(bytes));
			return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static com.nick22985.nbteditor.nbt.CompoundTag fromMc(CompoundTag tag) {
		if (tag == null) {
			return null;
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			NbtIo.write(tag, new DataOutputStream(bytes));
			NamedTag named = NbtIoUtil.readNamed(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
			return named.tag() instanceof com.nick22985.nbteditor.nbt.CompoundTag compound ? compound : new com.nick22985.nbteditor.nbt.CompoundTag();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Tag toMcTag(com.nick22985.nbteditor.nbt.Tag tag) {
		if (tag == null) {
			return null;
		}
		com.nick22985.nbteditor.nbt.CompoundTag wrapper = new com.nick22985.nbteditor.nbt.CompoundTag();
		wrapper.put(WRAP, tag);
		return toMc(wrapper).get(WRAP);
	}

	public static com.nick22985.nbteditor.nbt.Tag fromMcTag(Tag tag) {
		if (tag == null) {
			return null;
		}
		if (tag instanceof CompoundTag compound) {
			return fromMc(compound);
		}
		CompoundTag wrapper = new CompoundTag();
		wrapper.put(WRAP, tag);
		return fromMc(wrapper).get(WRAP);
	}
}
