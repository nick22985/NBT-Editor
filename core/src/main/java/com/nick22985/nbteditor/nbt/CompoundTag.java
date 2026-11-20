package com.nick22985.nbteditor.nbt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CompoundTag implements Tag {
	private final LinkedHashMap<String, Tag> entries;

	public CompoundTag() {
		this.entries = new LinkedHashMap<>();
	}

	public CompoundTag(Map<String, ? extends Tag> entries) {
		this.entries = new LinkedHashMap<>(entries);
	}

	public int size() {
		return entries.size();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public boolean contains(String key) {
		return entries.containsKey(key);
	}

	public boolean contains(String key, TagType type) {
		Tag tag = entries.get(key);
		return tag != null && tag.type() == type;
	}

	public boolean containsNumber(String key) {
		return entries.get(key) instanceof NumericTag;
	}

	public Tag get(String key) {
		return entries.get(key);
	}

	public Set<String> keys() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Set<Map.Entry<String, Tag>> entries() {
		return Collections.unmodifiableMap(entries).entrySet();
	}

	public Map<String, Tag> asMap() {
		return Collections.unmodifiableMap(entries);
	}

	public CompoundTag put(String key, Tag tag) {
		if (tag == null) {
			entries.remove(key);
		} else {
			entries.put(key, tag);
		}
		return this;
	}

	public CompoundTag putString(String key, String value) {
		return put(key, StringTag.of(value));
	}

	public CompoundTag putByte(String key, byte value) {
		return put(key, ByteTag.of(value));
	}

	public CompoundTag putBoolean(String key, boolean value) {
		return put(key, ByteTag.of(value));
	}

	public CompoundTag putShort(String key, short value) {
		return put(key, ShortTag.of(value));
	}

	public CompoundTag putInt(String key, int value) {
		return put(key, IntTag.of(value));
	}

	public CompoundTag putLong(String key, long value) {
		return put(key, LongTag.of(value));
	}

	public CompoundTag putFloat(String key, float value) {
		return put(key, FloatTag.of(value));
	}

	public CompoundTag putDouble(String key, double value) {
		return put(key, DoubleTag.of(value));
	}

	public int indexOf(String key) {
		int index = 0;
		for (String existing : entries.keySet()) {
			if (existing.equals(key)) {
				return index;
			}
			index++;
		}
		return -1;
	}

	public CompoundTag putAt(int index, String key, Tag tag) {
		if (index < 0 || index >= entries.size() || entries.containsKey(key)) {
			return put(key, tag);
		}
		LinkedHashMap<String, Tag> copy = new LinkedHashMap<>(entries);
		entries.clear();
		int position = 0;
		for (Map.Entry<String, Tag> entry : copy.entrySet()) {
			if (position++ == index) {
				entries.put(key, tag);
			}
			entries.put(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public Tag remove(String key) {
		return entries.remove(key);
	}

	public void clear() {
		entries.clear();
	}

	public boolean rename(String from, String to) {
		if (!entries.containsKey(from) || entries.containsKey(to)) {
			return false;
		}
		LinkedHashMap<String, Tag> copy = new LinkedHashMap<>(entries);
		entries.clear();
		for (Map.Entry<String, Tag> entry : copy.entrySet()) {
			entries.put(entry.getKey().equals(from) ? to : entry.getKey(), entry.getValue());
		}
		return true;
	}

	public CompoundTag getCompound(String key) {
		Tag tag = entries.get(key);
		return tag instanceof CompoundTag ? (CompoundTag) tag : null;
	}

	public CompoundTag getOrCreateCompound(String key) {
		Tag tag = entries.get(key);
		if (tag instanceof CompoundTag) {
			return (CompoundTag) tag;
		}
		CompoundTag created = new CompoundTag();
		entries.put(key, created);
		return created;
	}

	public ListTag getList(String key) {
		Tag tag = entries.get(key);
		return tag instanceof ListTag ? (ListTag) tag : null;
	}

	public ListTag getOrCreateList(String key) {
		Tag tag = entries.get(key);
		if (tag instanceof ListTag) {
			return (ListTag) tag;
		}
		ListTag created = new ListTag();
		entries.put(key, created);
		return created;
	}

	public String getString(String key) {
		Tag tag = entries.get(key);
		return tag instanceof StringTag ? ((StringTag) tag).value() : null;
	}

	public String getString(String key, String fallback) {
		String value = getString(key);
		return value == null ? fallback : value;
	}

	public NumericTag getNumber(String key) {
		Tag tag = entries.get(key);
		return tag instanceof NumericTag ? (NumericTag) tag : null;
	}

	public int getInt(String key, int fallback) {
		NumericTag tag = getNumber(key);
		return tag == null ? fallback : tag.intValue();
	}

	public long getLong(String key, long fallback) {
		NumericTag tag = getNumber(key);
		return tag == null ? fallback : tag.longValue();
	}

	public double getDouble(String key, double fallback) {
		NumericTag tag = getNumber(key);
		return tag == null ? fallback : tag.doubleValue();
	}

	public float getFloat(String key, float fallback) {
		NumericTag tag = getNumber(key);
		return tag == null ? fallback : tag.floatValue();
	}

	public boolean getBoolean(String key, boolean fallback) {
		NumericTag tag = getNumber(key);
		return tag == null ? fallback : tag.longValue() != 0;
	}

	public int[] getIntArray(String key) {
		Tag tag = entries.get(key);
		return tag instanceof IntArrayTag ? ((IntArrayTag) tag).value() : null;
	}

	public CompoundTag merge(CompoundTag other) {
		for (Map.Entry<String, Tag> entry : other.entries.entrySet()) {
			Tag existing = entries.get(entry.getKey());
			if (existing instanceof CompoundTag && entry.getValue() instanceof CompoundTag) {
				((CompoundTag) existing).merge((CompoundTag) entry.getValue());
			} else {
				entries.put(entry.getKey(), entry.getValue().copy());
			}
		}
		return this;
	}

	@Override
	public TagType type() {
		return TagType.COMPOUND;
	}

	@Override
	public CompoundTag copy() {
		CompoundTag copy = new CompoundTag();
		for (Map.Entry<String, Tag> entry : entries.entrySet()) {
			copy.entries.put(entry.getKey(), entry.getValue().copy());
		}
		return copy;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof CompoundTag && ((CompoundTag) o).entries.equals(entries);
	}

	@Override
	public int hashCode() {
		return entries.hashCode();
	}

	@Override
	public String toString() {
		return toSnbt();
	}
}
