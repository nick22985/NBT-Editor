package com.nick22985.nbteditor.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ListTag implements Tag, Iterable<Tag> {
	private final List<Tag> elements;

	public ListTag() {
		this.elements = new ArrayList<>();
	}

	public ListTag(Collection<? extends Tag> elements) {
		this.elements = new ArrayList<>(elements);
	}

	public static ListTag of(Tag... elements) {
		ListTag list = new ListTag();
		Collections.addAll(list.elements, elements);
		return list;
	}

	public static ListTag ofStrings(Collection<String> values) {
		ListTag list = new ListTag();
		for (String value : values) {
			list.add(StringTag.of(value));
		}
		return list;
	}

	public int size() {
		return elements.size();
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	public Tag get(int index) {
		return elements.get(index);
	}

	public CompoundTag getCompound(int index) {
		Tag tag = index >= 0 && index < elements.size() ? elements.get(index) : null;
		return tag instanceof CompoundTag ? (CompoundTag) tag : null;
	}

	public String getString(int index) {
		Tag tag = index >= 0 && index < elements.size() ? elements.get(index) : null;
		return tag instanceof StringTag ? ((StringTag) tag).value() : null;
	}

	public ListTag add(Tag tag) {
		elements.add(tag);
		return this;
	}

	public ListTag add(int index, Tag tag) {
		elements.add(index, tag);
		return this;
	}

	public Tag set(int index, Tag tag) {
		return elements.set(index, tag);
	}

	public Tag remove(int index) {
		return elements.remove(index);
	}

	public void clear() {
		elements.clear();
	}

	public List<Tag> elements() {
		return Collections.unmodifiableList(elements);
	}

	public TagType elementType() {
		if (elements.isEmpty()) {
			return TagType.END;
		}
		TagType first = elements.get(0).type();
		for (Tag element : elements) {
			if (element.type() != first) {
				return null;
			}
		}
		return first;
	}

	public boolean isHomogeneous() {
		return elementType() != null;
	}

	public TagType lastElementType() {
		return elements.isEmpty() ? TagType.END : elements.get(elements.size() - 1).type();
	}

	@Override
	public Iterator<Tag> iterator() {
		return elements().iterator();
	}

	@Override
	public TagType type() {
		return TagType.LIST;
	}

	@Override
	public ListTag copy() {
		ListTag copy = new ListTag();
		for (Tag element : elements) {
			copy.elements.add(element.copy());
		}
		return copy;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ListTag && ((ListTag) o).elements.equals(elements);
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public String toString() {
		return toSnbt();
	}
}
