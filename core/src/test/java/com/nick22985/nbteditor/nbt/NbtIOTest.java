package com.nick22985.nbteditor.nbt;

import com.nick22985.nbteditor.util.NbtIoUtil;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class NbtIOTest {
	private static CompoundTag sample() {
		CompoundTag tag = new CompoundTag();
		tag.putString("s", "text").putByte("b", (byte) 3).putShort("sh", (short) 4).putInt("i", 5).putLong("l", 6L).putFloat("f", 7.5f).putDouble("d", 8.25);
		tag.put("ba", new ByteArrayTag(new byte[] {1, 2}));
		tag.put("ia", new IntArrayTag(new int[] {3, 4}));
		tag.put("la", new LongArrayTag(new long[] {5L}));
		tag.put("list", ListTag.of(StringTag.of("a"), StringTag.of("b")));
		tag.put("empty", new ListTag());
		tag.getOrCreateCompound("nested").putInt("x", 1);
		return tag;
	}

	@Test
	void roundTripEveryCompression() throws IOException {
		CompoundTag tag = sample();
		for (NbtCompression compression : NbtCompression.values()) {
			byte[] bytes = NbtIoUtil.write(new NamedTag("root", tag), compression);
			assertEquals(compression, NbtCompression.detect(bytes), compression.name());
			NamedTag back = NbtIoUtil.readAuto(bytes);
			assertEquals("root", back.name());
			assertEquals(tag, back.tag(), compression.name());
		}
	}

	@Test
	void heterogeneousListsAreWrapped() throws IOException {
		CompoundTag tag = new CompoundTag();
		tag.put("mixed", ListTag.of(IntTag.of(1), StringTag.of("two")));
		CompoundTag back = (CompoundTag) NbtIoUtil.fromBytes(NbtIoUtil.toBytes(tag));
		assertEquals(tag, back);
	}
}
