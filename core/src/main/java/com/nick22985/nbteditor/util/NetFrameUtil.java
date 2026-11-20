package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.net.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NetFrameUtil {
	private static final int HEADER_ALLOWANCE = 64;

	private NetFrameUtil() {
	}

	public static List<byte[]> split(int messageId, String type, byte[] payload, int frameSize) {
		int chunk = Math.max(1, frameSize - HEADER_ALLOWANCE - type.length() * 3);
		int parts = Math.max(1, (payload.length + chunk - 1) / chunk);
		List<byte[]> frames = new ArrayList<>(parts);
		for (int part = 0; part < parts; part++) {
			int start = part * chunk;
			int end = Math.min(payload.length, start + chunk);
			byte[] slice = Arrays.copyOfRange(payload, start, end);
			frames.add(new Frame(messageId, part, parts, part == 0 ? type : null, slice).encode());
		}
		return frames;
	}
}
