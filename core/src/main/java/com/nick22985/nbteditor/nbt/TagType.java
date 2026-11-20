package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum TagType {
	END(0, "TAG_End", ""),
	BYTE(1, "TAG_Byte", "B"),
	SHORT(2, "TAG_Short", "S"),
	INT(3, "TAG_Int", "I"),
	LONG(4, "TAG_Long", "L"),
	FLOAT(5, "TAG_Float", "F"),
	DOUBLE(6, "TAG_Double", "D"),
	BYTE_ARRAY(7, "TAG_Byte_Array", "[B"),
	STRING(8, "TAG_String", "S"),
	LIST(9, "TAG_List", "[]"),
	COMPOUND(10, "TAG_Compound", "{}"),
	INT_ARRAY(11, "TAG_Int_Array", "[I"),
	LONG_ARRAY(12, "TAG_Long_Array", "[L");

	private static final TagType[] BY_ID = values();

	private final int id;
	private final String nbtName;
	private final String badge;

	TagType(int id, String nbtName, String badge) {
		this.id = id;
		this.nbtName = nbtName;
		this.badge = badge;
	}

	public int id() {
		return id;
	}

	public String nbtName() {
		return nbtName;
	}

	public String badge() {
		return badge;
	}

	public boolean isNumeric() {
		return id >= 1 && id <= 6;
	}

	public boolean isInteger() {
		return id >= 1 && id <= 4;
	}

	public boolean isArray() {
		return this == BYTE_ARRAY || this == INT_ARRAY || this == LONG_ARRAY;
	}

	public boolean isContainer() {
		return this == LIST || this == COMPOUND || isArray();
	}

	public static TagType byId(int id) {
		if (id < 0 || id >= BY_ID.length) {
			throw new IllegalArgumentException("Unknown tag type " + id);
		}
		return BY_ID[id];
	}
}
