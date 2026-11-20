package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NamedTag;
import com.nick22985.nbteditor.nbt.NbtCompression;
import com.nick22985.nbteditor.nbt.RegionChunk;
import com.nick22985.nbteditor.nbt.RegionPos;
import com.nick22985.nbteditor.nbt.Tag;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RegionUtil {
	public static final int CHUNKS = 1024;
	public static final int SECTOR = 4096;
	public static final int GZIP = 1;
	public static final int ZLIB = 2;
	public static final int NONE = 3;
	public static final int EXTERNAL = 128;
	private static final int MAX_SECTORS = 255;

	private RegionUtil() {
	}

	public static int index(int chunkX, int chunkZ) {
		return (chunkX & 31) + (chunkZ & 31) * 32;
	}

	public static List<RegionChunk> read(Path file) throws IOException {
		byte[] bytes = Files.readAllBytes(file);
		return read(bytes, file.getParent(), RegionPos.parse(file.getFileName().toString()));
	}

	public static List<RegionChunk> read(byte[] bytes, Path dir, RegionPos pos) throws IOException {
		List<RegionChunk> chunks = new ArrayList<>();
		if (bytes.length == 0) {
			return chunks;
		}
		if (bytes.length < SECTOR * 2) {
			throw new IOException("The region header is cut short");
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		for (int index = 0; index < CHUNKS; index++) {
			int location = buffer.getInt(index * 4);
			int offset = location >>> 8;
			int sectors = location & 0xFF;
			if (offset < 2 || sectors == 0) {
				continue;
			}
			int timestamp = buffer.getInt(SECTOR + index * 4);
			int start = offset * SECTOR;
			if (start + 5 > bytes.length) {
				continue;
			}
			int length = buffer.getInt(start);
			int type = bytes[start + 4] & 0xFF;
			if ((type & EXTERNAL) != 0) {
				Path external = dir == null || pos == null ? null : dir.resolve(externalName(pos, index));
				byte[] data = external != null && Files.isRegularFile(external) ? Files.readAllBytes(external) : null;
				chunks.add(new RegionChunk(index, timestamp, data == null ? type : type & ~EXTERNAL, data == null ? new byte[0] : data));
				continue;
			}
			if (length < 1 || start + 4 + length > bytes.length) {
				continue;
			}
			byte[] data = new byte[length - 1];
			System.arraycopy(bytes, start + 5, data, 0, data.length);
			chunks.add(new RegionChunk(index, timestamp, type, data));
		}
		return chunks;
	}

	public static String externalName(RegionPos pos, int index) {
		return "c." + pos.chunkX(index) + "." + pos.chunkZ(index) + ".mcc";
	}

	public static boolean readable(RegionChunk chunk) {
		return (chunk.compression() & EXTERNAL) == 0 && compression(chunk.compression()) != null;
	}

	private static NbtCompression compression(int type) {
		switch (type) {
			case GZIP:
				return NbtCompression.GZIP;
			case ZLIB:
				return NbtCompression.ZLIB;
			case NONE:
				return NbtCompression.NONE;
			default:
				return null;
		}
	}

	public static CompoundTag decode(RegionChunk chunk) throws IOException {
		NbtCompression compression = compression(chunk.compression());
		if ((chunk.compression() & EXTERNAL) != 0) {
			throw new IOException("The chunk's .mcc file is missing");
		}
		if (compression == null) {
			throw new IOException("Unsupported chunk compression " + chunk.compression());
		}
		Tag tag = NbtIoUtil.read(new ByteArrayInputStream(chunk.data()), compression).tag();
		if (!(tag instanceof CompoundTag)) {
			throw new IOException("The chunk isn't a compound");
		}
		return (CompoundTag) tag;
	}

	public static RegionChunk encode(int index, int timestamp, int compression, CompoundTag tag) throws IOException {
		int type = compression(compression) == null ? ZLIB : compression;
		return new RegionChunk(index, timestamp, type, NbtIoUtil.write(new NamedTag("", tag), compression(type)));
	}

	public static List<RegionChunk> replace(List<RegionChunk> chunks, RegionChunk chunk) {
		List<RegionChunk> out = new ArrayList<>();
		for (RegionChunk existing : chunks) {
			if (existing.index() != chunk.index()) {
				out.add(existing);
			}
		}
		out.add(chunk);
		return out;
	}

	public static void write(Path file, List<RegionChunk> chunks) throws IOException {
		RegionPos pos = RegionPos.parse(file.getFileName().toString());
		List<RegionChunk> sorted = new ArrayList<>(chunks);
		sorted.sort(Comparator.comparingInt(RegionChunk::index));
		ByteBuffer header = ByteBuffer.allocate(SECTOR * 2);
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		int sector = 2;
		for (RegionChunk chunk : sorted) {
			byte[] data = chunk.data();
			int type = chunk.compression();
			Path external = pos == null ? null : file.resolveSibling(externalName(pos, chunk.index()));
			if ((type & EXTERNAL) != 0) {
				data = new byte[0];
			} else if (sectors(data.length) > MAX_SECTORS) {
				if (external == null) {
					throw new IOException("The chunk is too big for a region file without coordinates");
				}
				DataFileUtil.writeAtomic(external, data);
				data = new byte[0];
				type |= EXTERNAL;
			} else if (external != null) {
				Files.deleteIfExists(external);
			}
			int count = sectors(data.length);
			ByteBuffer payload = ByteBuffer.allocate(count * SECTOR);
			payload.putInt(data.length + 1);
			payload.put((byte) type);
			payload.put(data);
			body.write(payload.array());
			header.putInt(chunk.index() * 4, sector << 8 | count);
			header.putInt(SECTOR + chunk.index() * 4, chunk.timestamp());
			sector += count;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(header.array());
		body.writeTo(out);
		DataFileUtil.writeAtomic(file, out.toByteArray());
	}

	private static int sectors(int dataLength) {
		return (dataLength + 5 + SECTOR - 1) / SECTOR;
	}
}
