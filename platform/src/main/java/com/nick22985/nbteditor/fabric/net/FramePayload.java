package com.nick22985.nbteditor.fabric.net;

//? if >=1.20.5 {
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.net.Transport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record FramePayload(byte[] data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FramePayload> TYPE = new CustomPacketPayload.Type<>(McIdUtil.of("nbtedit", "frame"));
	public static final StreamCodec<FriendlyByteBuf, FramePayload> CODEC = StreamCodec.of((buf, payload) -> buf.writeByteArray(payload.data()), buf -> new FramePayload(buf.readByteArray(Transport.MAX_CLIENTBOUND_FRAME)));

	@Override
	public CustomPacketPayload.Type<FramePayload> type() {
		return TYPE;
	}
}
//?}
