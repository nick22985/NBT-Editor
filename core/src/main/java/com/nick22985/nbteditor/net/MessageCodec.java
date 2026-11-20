package com.nick22985.nbteditor.net;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface MessageCodec<T> {
	void write(ByteWriter out, T message);

	T read(ByteReader in);

	static <T> MessageCodec<T> of(BiConsumer<ByteWriter, T> writer, Function<ByteReader, T> reader) {
		return new FunctionalMessageCodec<>(writer, reader);
	}
}
