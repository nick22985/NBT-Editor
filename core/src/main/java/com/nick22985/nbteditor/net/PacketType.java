package com.nick22985.nbteditor.net;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PacketType<T>(String id, Direction direction, MessageCodec<T> codec) {
	public static <T> PacketType<T> serverbound(String id, MessageCodec<T> codec) {
		return new PacketType<>(id, Direction.SERVERBOUND, codec);
	}

	public static <T> PacketType<T> clientbound(String id, MessageCodec<T> codec) {
		return new PacketType<>(id, Direction.CLIENTBOUND, codec);
	}
}
