package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.DoubleTag;
import com.nick22985.nbteditor.nbt.EndTag;
import com.nick22985.nbteditor.nbt.FloatTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.LongTag;
import com.nick22985.nbteditor.nbt.NamedTag;
import com.nick22985.nbteditor.nbt.NbtCompression;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.ShortTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.nbt.TagType;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtIoUtil {
	public static final int MAX_DEPTH = 512;

	private NbtIoUtil() {
	}

	public static InputStream decompress(InputStream in, NbtCompression compression) throws IOException {
		switch (compression) {
			case GZIP:
				return new BufferedInputStream(new GZIPInputStream(in));
			case ZLIB:
				return new BufferedInputStream(new InflaterInputStream(in));
			default:
				return in;
		}
	}

	public static OutputStream compress(OutputStream out, NbtCompression compression) throws IOException {
		switch (compression) {
			case GZIP:
				return new GZIPOutputStream(out);
			case ZLIB:
				return new DeflaterOutputStream(out);
			default:
				return out;
		}
	}

	public static NamedTag readAuto(byte[] data) throws IOException {
		NbtCompression compression = NbtCompression.detect(data);
		try (InputStream in = decompress(new ByteArrayInputStream(data), compression)) {
			return readNamed(new DataInputStream(in));
		}
	}

	public static NamedTag read(InputStream in, NbtCompression compression) throws IOException {
		return readNamed(new DataInputStream(decompress(in, compression)));
	}

	public static byte[] write(NamedTag named, NbtCompression compression) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (OutputStream out = compress(bytes, compression)) {
			DataOutputStream data = new DataOutputStream(out);
			writeNamed(named, data);
			data.flush();
		}
		return bytes.toByteArray();
	}

	public static NamedTag readNamed(DataInput in) throws IOException {
		int id = in.readUnsignedByte();
		if (id == 0) {
			return new NamedTag("", EndTag.INSTANCE);
		}
		String name = in.readUTF();
		return new NamedTag(name, readPayload(in, TagType.byId(id), 0));
	}

	public static void writeNamed(NamedTag named, DataOutput out) throws IOException {
		out.writeByte(named.tag().type().id());
		if (named.tag().type() == TagType.END) {
			return;
		}
		out.writeUTF(named.name());
		writePayload(named.tag(), out);
	}

	public static Tag readUnnamed(DataInput in) throws IOException {
		int id = in.readUnsignedByte();
		if (id == 0) {
			return EndTag.INSTANCE;
		}
		return readPayload(in, TagType.byId(id), 0);
	}

	public static void writeUnnamed(Tag tag, DataOutput out) throws IOException {
		out.writeByte(tag.type().id());
		if (tag.type() != TagType.END) {
			writePayload(tag, out);
		}
	}

	public static Tag readPayload(DataInput in, TagType type, int depth) throws IOException {
		if (depth > MAX_DEPTH) {
			throw new IOException("Tried to read NBT tag with too high complexity, depth > " + MAX_DEPTH);
		}
		switch (type) {
			case BYTE:
				return ByteTag.of(in.readByte());
			case SHORT:
				return ShortTag.of(in.readShort());
			case INT:
				return IntTag.of(in.readInt());
			case LONG:
				return LongTag.of(in.readLong());
			case FLOAT:
				return FloatTag.of(in.readFloat());
			case DOUBLE:
				return DoubleTag.of(in.readDouble());
			case STRING:
				return StringTag.of(in.readUTF());
			case BYTE_ARRAY: {
				byte[] value = new byte[checkLength(in.readInt())];
				in.readFully(value);
				return new ByteArrayTag(value);
			}
			case INT_ARRAY: {
				int[] value = new int[checkLength(in.readInt())];
				for (int i = 0; i < value.length; i++) {
					value[i] = in.readInt();
				}
				return new IntArrayTag(value);
			}
			case LONG_ARRAY: {
				long[] value = new long[checkLength(in.readInt())];
				for (int i = 0; i < value.length; i++) {
					value[i] = in.readLong();
				}
				return new LongArrayTag(value);
			}
			case LIST: {
				TagType elementType = TagType.byId(in.readUnsignedByte());
				int length = checkLength(in.readInt());
				ListTag list = new ListTag();
				for (int i = 0; i < length; i++) {
					Tag element = readPayload(in, elementType, depth + 1);
					list.add(elementType == TagType.COMPOUND ? unwrap((CompoundTag) element) : element);
				}
				return list;
			}
			case COMPOUND: {
				CompoundTag compound = new CompoundTag();
				while (true) {
					int id = in.readUnsignedByte();
					if (id == 0) {
						return compound;
					}
					String key = in.readUTF();
					compound.put(key, readPayload(in, TagType.byId(id), depth + 1));
				}
			}
			default:
				throw new IOException("Unexpected tag type " + type);
		}
	}

	private static int checkLength(int length) throws IOException {
		if (length < 0) {
			throw new IOException("Negative length " + length);
		}
		return length;
	}

	private static Tag unwrap(CompoundTag compound) {
		if (compound.size() == 1 && compound.contains("")) {
			return compound.get("");
		}
		return compound;
	}

	public static void writePayload(Tag tag, DataOutput out) throws IOException {
		switch (tag.type()) {
			case BYTE:
				out.writeByte(((NumericTag) tag).byteValue());
				break;
			case SHORT:
				out.writeShort(((NumericTag) tag).shortValue());
				break;
			case INT:
				out.writeInt(((NumericTag) tag).intValue());
				break;
			case LONG:
				out.writeLong(((NumericTag) tag).longValue());
				break;
			case FLOAT:
				out.writeFloat(((NumericTag) tag).floatValue());
				break;
			case DOUBLE:
				out.writeDouble(((NumericTag) tag).doubleValue());
				break;
			case STRING:
				out.writeUTF(((StringTag) tag).value());
				break;
			case BYTE_ARRAY: {
				byte[] value = ((ByteArrayTag) tag).value();
				out.writeInt(value.length);
				out.write(value);
				break;
			}
			case INT_ARRAY: {
				int[] value = ((IntArrayTag) tag).value();
				out.writeInt(value.length);
				for (int v : value) {
					out.writeInt(v);
				}
				break;
			}
			case LONG_ARRAY: {
				long[] value = ((LongArrayTag) tag).value();
				out.writeInt(value.length);
				for (long v : value) {
					out.writeLong(v);
				}
				break;
			}
			case LIST: {
				ListTag list = (ListTag) tag;
				TagType elementType = list.elementType();
				boolean wrap = elementType == null;
				if (elementType == TagType.COMPOUND) {
					for (Tag element : list) {
						CompoundTag compound = (CompoundTag) element;
						if (compound.size() == 1 && compound.contains("")) {
							wrap = true;
							break;
						}
					}
				}
				TagType written = wrap ? TagType.COMPOUND : elementType;
				out.writeByte(written.id());
				out.writeInt(list.size());
				for (Tag element : list) {
					if (wrap && (element.type() != TagType.COMPOUND || isWrapperShaped((CompoundTag) element))) {
						CompoundTag wrapper = new CompoundTag();
						wrapper.put("", element);
						writePayload(wrapper, out);
					} else {
						writePayload(element, out);
					}
				}
				break;
			}
			case COMPOUND: {
				for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
					out.writeByte(entry.getValue().type().id());
					out.writeUTF(entry.getKey());
					writePayload(entry.getValue(), out);
				}
				out.writeByte(0);
				break;
			}
			default:
				break;
		}
	}

	private static boolean isWrapperShaped(CompoundTag compound) {
		return compound.size() == 1 && compound.contains("");
	}

	public static byte[] toBytes(Tag tag) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			writeUnnamed(tag, out);
			out.flush();
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Tag fromBytes(byte[] data) throws IOException {
		return readUnnamed(new DataInputStream(new ByteArrayInputStream(data)));
	}
}
