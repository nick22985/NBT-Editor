package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Field<T> {
	String id();

	default boolean supported(GameVersion version) {
		return true;
	}

	FieldValue<T> read(CompoundTag subject, GameVersion version);

	void write(CompoundTag subject, GameVersion version, T value);

	default T get(CompoundTag subject, GameVersion version) {
		return read(subject, version).value();
	}

	default boolean has(CompoundTag subject, GameVersion version) {
		return read(subject, version).present();
	}

	default void remove(CompoundTag subject, GameVersion version) {
		write(subject, version, null);
	}
}
