package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.RegionChunk;
import com.nick22985.nbteditor.nbt.RegionPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class RegionUtilTest {
	@TempDir
	Path dir;

	private static CompoundTag chunk(int x, int z) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("xPos", x).putInt("zPos", z).putInt("DataVersion", 3955);
		return tag;
	}

	@Test
	void parsesRegionNames() {
		RegionPos pos = RegionPos.parse("r.-1.2.mca");
		assertEquals(new RegionPos(-1, 2), pos);
		assertEquals(-32 + 5, pos.chunkX(RegionUtil.index(5, 3)));
		assertEquals(64 + 3, pos.chunkZ(RegionUtil.index(5, 3)));
		assertNull(RegionPos.parse("level.dat"));
	}

	@Test
	void roundTripsChunks() throws IOException {
		Path file = this.dir.resolve("r.0.0.mca");
		RegionChunk a = RegionUtil.encode(RegionUtil.index(0, 0), 100, RegionUtil.ZLIB, chunk(0, 0));
		RegionChunk b = RegionUtil.encode(RegionUtil.index(31, 31), 200, RegionUtil.GZIP, chunk(31, 31));
		RegionUtil.write(file, List.of(b, a));
		List<RegionChunk> read = RegionUtil.read(file);
		assertEquals(2, read.size());
		assertEquals(0, read.get(0).index());
		assertEquals(100, read.get(0).timestamp());
		assertEquals(chunk(0, 0), RegionUtil.decode(read.get(0)));
		assertEquals(1023, read.get(1).index());
		assertEquals(RegionUtil.GZIP, read.get(1).compression());
		assertEquals(chunk(31, 31), RegionUtil.decode(read.get(1)));
		assertEquals(0, Files.size(file) % RegionUtil.SECTOR);
	}

	@Test
	void largeChunksGoExternal() throws IOException {
		Path file = this.dir.resolve("r.1.0.mca");
		byte[] noise = new byte[1200 * 1024];
		new Random(7).nextBytes(noise);
		CompoundTag big = chunk(32, 0);
		big.put("noise", new ByteArrayTag(noise));
		RegionUtil.write(file, List.of(RegionUtil.encode(0, 1, RegionUtil.NONE, big)));
		assertTrue(Files.isRegularFile(this.dir.resolve("c.32.0.mcc")));
		List<RegionChunk> read = RegionUtil.read(file);
		assertEquals(1, read.size());
		assertTrue(RegionUtil.readable(read.get(0)));
		assertEquals(big, RegionUtil.decode(read.get(0)));
		RegionUtil.write(file, List.of(RegionUtil.encode(0, 1, RegionUtil.NONE, chunk(32, 0))));
		assertFalse(Files.exists(this.dir.resolve("c.32.0.mcc")));
	}

	@Test
	void replaceSwapsByIndex() throws IOException {
		RegionChunk a = RegionUtil.encode(4, 1, RegionUtil.ZLIB, chunk(4, 0));
		RegionChunk b = RegionUtil.encode(4, 2, RegionUtil.ZLIB, chunk(5, 0));
		List<RegionChunk> out = RegionUtil.replace(List.of(a), b);
		assertEquals(1, out.size());
		assertEquals(2, out.get(0).timestamp());
	}
}
