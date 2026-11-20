package com.nick22985.nbteditor.net;

import com.nick22985.nbteditor.log.StdoutLog;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.util.NetFrameUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class NetTest {
	@Test
	void primitivesRoundTrip() {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "minecraft:stone").putInt("count", 3);
		UUID uuid = UUID.randomUUID();
		byte[] data = new ByteWriter().writeVarInt(300).writeVarInt(-1).writeVarLong(Long.MIN_VALUE).writeBoolean(true).writeString("héllo ☃").writeNullableString(null)
			.writeUuid(uuid).writeDouble(1.5).writeFloat(2.5f).writeLong(42L).writeTag(tag).writeEnum(Direction.CLIENTBOUND).toByteArray();
		ByteReader in = new ByteReader(data);
		assertEquals(300, in.readVarInt());
		assertEquals(-1, in.readVarInt());
		assertEquals(Long.MIN_VALUE, in.readVarLong());
		assertTrue(in.readBoolean());
		assertEquals("héllo ☃", in.readString());
		assertNull(in.readNullableString());
		assertEquals(uuid, in.readUuid());
		assertEquals(1.5, in.readDouble());
		assertEquals(2.5f, in.readFloat());
		assertEquals(42L, in.readLong());
		assertEquals(tag, in.readTag());
		assertEquals(Direction.CLIENTBOUND, in.readEnum(Direction.class));
		assertEquals(0, in.remaining());
	}

	@Test
	void readPastEndFails() {
		ByteReader in = new ByteReader(new byte[] {1});
		in.readByte();
		assertThrows(NetException.class, in::readByte);
	}

	@Test
	void splitAndAssemble() {
		byte[] payload = new byte[100_000];
		new Random(7).nextBytes(payload);
		List<byte[]> frames = NetFrameUtil.split(5, "nbtedit:test", payload, Transport.MAX_SERVERBOUND_FRAME);
		assertTrue(frames.size() > 3);
		FrameAssembler assembler = new FrameAssembler();
		AssembledMessage message = null;
		for (byte[] frame : frames) {
			assertTrue(frame.length <= Transport.MAX_SERVERBOUND_FRAME);
			assertNull(message);
			message = assembler.accept(Frame.decode(frame));
		}
		assertEquals("nbtedit:test", message.type());
		assertArrayEquals(payload, message.payload());
	}

	@Test
	void outOfOrderFramesFail() {
		List<byte[]> frames = NetFrameUtil.split(1, "t", new byte[70_000], Transport.MAX_SERVERBOUND_FRAME);
		FrameAssembler assembler = new FrameAssembler();
		assertThrows(NetException.class, () -> assembler.accept(Frame.decode(frames.get(1))));
	}

	@Test
	void serviceDeliversLargeMessages() {
		LoopbackTransport transport = new LoopbackTransport();
		NetService net = new NetService(transport, new StdoutLog("test"));
		PacketType<String> echo = PacketType.serverbound("nbtedit:echo", MessageCodec.of(ByteWriter::writeString, ByteReader::readString));
		PacketType<String> reply = PacketType.clientbound("nbtedit:reply", MessageCodec.of(ByteWriter::writeString, ByteReader::readString));
		List<String> received = new ArrayList<>();
		net.registerServer(echo, (sender, message) -> net.sendToClient(sender, reply, message.toUpperCase()));
		net.registerClient(reply, received::add);
		String big = "a".repeat(80_000);
		net.sendToServer(echo, big);
		assertTrue(transport.serverbound.size() > 1);
		assertEquals(List.of(big.toUpperCase()), received);
	}

	@Test
	void unknownPacketsAreIgnored() {
		LoopbackTransport transport = new LoopbackTransport();
		NetService net = new NetService(transport, new StdoutLog("test"));
		PacketType<String> stray = PacketType.serverbound("nbtedit:stray", MessageCodec.of(ByteWriter::writeString, ByteReader::readString));
		net.sendToServer(stray, "x");
		assertEquals(1, transport.serverbound.size());
	}
}
