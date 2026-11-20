package com.nick22985.nbteditor.net;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record FunctionalMessageCodec<T>(BiConsumer<ByteWriter, T> writer, Function<ByteReader, T> reader) implements MessageCodec<T> {
	@Override
	public void write(ByteWriter out, T message) {
		this.writer.accept(out, message);
	}

	@Override
	public T read(ByteReader in) {
		return this.reader.apply(in);
	}
}
