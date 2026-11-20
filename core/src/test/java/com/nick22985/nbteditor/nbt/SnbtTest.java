package com.nick22985.nbteditor.nbt;

import com.nick22985.nbteditor.util.SnbtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class SnbtTest {
	private static Tag legacy(String input) throws SnbtException {
		return SnbtUtil.parse(input, SnbtDialect.LEGACY);
	}

	private static Tag modern(String input) throws SnbtException {
		return SnbtUtil.parse(input, SnbtDialect.MODERN);
	}

	@Test
	void legacyScalars() throws SnbtException {
		assertEquals(ByteTag.of((byte) 1), legacy("1b"));
		assertEquals(ByteTag.of(true), legacy("true"));
		assertEquals(ShortTag.of((short) 7), legacy("7s"));
		assertEquals(IntTag.of(42), legacy("42"));
		assertEquals(LongTag.of(9L), legacy("9L"));
		assertEquals(FloatTag.of(1.5f), legacy("1.5f"));
		assertEquals(DoubleTag.of(2.0), legacy("2.0"));
		assertEquals(DoubleTag.of(2.0), legacy("2d"));
		assertEquals(StringTag.of("hello"), legacy("hello"));
	}

	@Test
	void legacyOutOfRangeBecomesString() throws SnbtException {
		assertEquals(StringTag.of("300b"), legacy("300b"));
		assertEquals(StringTag.of("99999999999"), legacy("99999999999"));
	}

	@Test
	void legacyRejectsUnknownEscapes() {
		assertThrows(SnbtException.class, () -> legacy("\"a\\nb\""));
	}

	@Test
	void modernNumbers() throws SnbtException {
		assertEquals(IntTag.of(1000000), modern("1_000_000"));
		assertEquals(IntTag.of(255), modern("0xFF"));
		assertEquals(ByteTag.of((byte) -1), modern("0xFFub"));
		assertEquals(ByteTag.of((byte) 5), modern("0b101b"));
		assertEquals(LongTag.of(1L), modern("1sl"));
		assertEquals(StringTag.of("a\nb"), modern("\"a\\nb\""));
		assertEquals(StringTag.of("A"), modern("'\\x41'"));
		assertThrows(SnbtException.class, () -> modern("300b"));
	}

	@Test
	void modernOperationsAndMixedLists() throws SnbtException {
		assertEquals(ByteTag.ONE, modern("bool(1)"));
		Tag uuid = modern("uuid(f81d4fae-7dec-11d0-a765-00a0c91e6bf6)");
		assertInstanceOf(IntArrayTag.class, uuid);
		ListTag mixed = (ListTag) modern("[1, \"two\", {three: 3}]");
		assertEquals(3, mixed.size());
		assertEquals(null, mixed.elementType());
	}

	@Test
	void specialNumbers() throws SnbtException {
		assertTrue(Double.isNaN(((DoubleTag) legacy("NaNd")).value()));
		assertEquals(Double.POSITIVE_INFINITY, ((DoubleTag) legacy("Infinityd")).value());
		assertEquals(Float.NEGATIVE_INFINITY, ((FloatTag) modern("-Infinityf")).value());
		assertEquals(Double.POSITIVE_INFINITY, ((DoubleTag) legacy("1e1000d")).value());
	}

	@Test
	void arraysAndCompounds() throws SnbtException {
		CompoundTag tag = SnbtUtil.parseCompound("{a:[B;1b,2b],b:[I;1,2,3],c:[L;4L],d:{e:\"f\"},}", SnbtDialect.LEGACY);
		assertEquals(2, ((ByteArrayTag) tag.get("a")).size());
		assertEquals(3, ((IntArrayTag) tag.get("b")).size());
		assertEquals(4L, ((LongArrayTag) tag.get("c")).get(0));
		assertEquals("f", tag.getCompound("d").getString("e"));
	}

	@Test
	void trailingDataRejected() {
		assertThrows(SnbtException.class, () -> legacy("{a:1} junk"));
	}

	@Test
	void roundTrip() throws SnbtException {
		String input = "{name:\"Steve's \\\"hat\\\"\",list:[1,2,3],nested:{x:1.5f,y:-2L},arr:[I;1,2],\"weird key\":1b}";
		for (SnbtDialect dialect : SnbtDialect.values()) {
			Tag parsed = SnbtUtil.parse(input, dialect);
			String written = SnbtWriter.compact(dialect).write(parsed);
			assertEquals(parsed, SnbtUtil.parse(written, dialect), dialect.name());
			String pretty = SnbtWriter.pretty(dialect).write(parsed);
			assertEquals(parsed, SnbtUtil.parse(pretty, dialect), dialect.name());
		}
	}

	@Test
	void gameSafeInfinity() throws SnbtException {
		CompoundTag tag = new CompoundTag().putDouble("d", Double.POSITIVE_INFINITY).putFloat("f", Float.NEGATIVE_INFINITY);
		String written = SnbtWriter.forCommands(SnbtDialect.LEGACY).write(tag);
		assertTrue(written.contains("1.0e1000d"), written);
		assertEquals(tag, SnbtUtil.parse(written, SnbtDialect.LEGACY));
	}

	@Test
	void sharedEmptyInstancesAreSafe() {
		CompoundTag a = new CompoundTag();
		a.getOrCreateList("x").add(StringTag.EMPTY);
		CompoundTag b = a.copy();
		b.getList("x").add(IntTag.of(1));
		assertEquals(1, a.getList("x").size());
		assertEquals(2, b.getList("x").size());
	}

	@Test
	void compoundOrderingHelpers() {
		CompoundTag tag = new CompoundTag().putInt("a", 1).putInt("b", 2).putInt("c", 3);
		tag.remove("b");
		tag.putAt(1, "b", IntTag.of(2));
		assertEquals(1, tag.indexOf("b"));
		assertTrue(tag.rename("a", "z"));
		assertEquals(0, tag.indexOf("z"));
	}
}
