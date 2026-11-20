package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.DyeColor;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.SignText;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SignUtil {
	public static final int SIGN_WIDTH = 90;
	public static final int HANGING_WIDTH = 60;
	public static final String SIGN = "minecraft:sign";
	public static final String HANGING = "minecraft:hanging_sign";

	private SignUtil() {
	}

	public static boolean isSign(String id) {
		return id != null && id.endsWith("_sign");
	}

	public static boolean isHanging(String id) {
		return id != null && id.contains("hanging_sign");
	}

	public static String blockEntityId(String id) {
		return isHanging(id) ? HANGING : SIGN;
	}

	public static int maxWidth(String id) {
		return isHanging(id) ? HANGING_WIDTH : SIGN_WIDTH;
	}

	public static int textColor(SignText text) {
		DyeColor dye = DyeColor.of(text.color());
		int rgb = (dye == null ? DyeColor.BLACK : dye).textColor();
		if (text.glowing()) {
			return dye == null || dye == DyeColor.BLACK ? 0xF0EBCC : rgb;
		}
		return ColorUtil.rgb((int) (ColorUtil.red(rgb) * 0.4), (int) (ColorUtil.green(rgb) * 0.4), (int) (ColorUtil.blue(rgb) * 0.4));
	}

	public static SignText read(CompoundTag blockEntity, boolean front, GameVersion version) {
		TextDialect dialect = TextDialect.of(version);
		List<Text> lines = new ArrayList<>();
		if (!Feature.SIGN_TWO_SIDES.in(version)) {
			for (int i = 1; i <= SignText.LINES; i++) {
				lines.add(line(blockEntity.get("Text" + i), dialect));
			}
			return new SignText(lines, blockEntity.getString("Color", DyeColor.BLACK.id()), blockEntity.getBoolean("GlowingText", false));
		}
		CompoundTag side = blockEntity.getCompound(front ? "front_text" : "back_text");
		if (side == null) {
			return SignText.EMPTY;
		}
		ListTag messages = side.getList("messages");
		for (int i = 0; i < SignText.LINES; i++) {
			lines.add(line(messages != null && i < messages.size() ? messages.get(i) : null, dialect));
		}
		return new SignText(lines, side.getString("color", DyeColor.BLACK.id()), side.getBoolean("has_glowing_text", false));
	}

	private static Text line(Tag tag, TextDialect dialect) {
		Text text = tag == null ? null : TextCodecUtil.fromStored(tag, dialect);
		return text == null ? Text.EMPTY : text;
	}

	public static void write(CompoundTag blockEntity, boolean front, GameVersion version, SignText text) {
		TextDialect dialect = TextDialect.of(version);
		if (!Feature.SIGN_TWO_SIDES.in(version)) {
			for (int i = 0; i < SignText.LINES; i++) {
				blockEntity.put("Text" + (i + 1), TextCodecUtil.toStored(text.lines().get(i), dialect));
				blockEntity.remove("FilteredText" + (i + 1));
			}
			blockEntity.putString("Color", text.color());
			blockEntity.putBoolean("GlowingText", text.glowing());
			return;
		}
		CompoundTag side = blockEntity.getOrCreateCompound(front ? "front_text" : "back_text");
		ListTag messages = new ListTag();
		for (Text line : text.lines()) {
			messages.add(TextCodecUtil.toStored(line, dialect));
		}
		side.put("messages", messages);
		side.remove("filtered_messages");
		side.putString("color", text.color());
		side.putBoolean("has_glowing_text", text.glowing());
	}

	public static boolean waxed(CompoundTag blockEntity) {
		return blockEntity.getBoolean("is_waxed", false);
	}

	public static void waxed(CompoundTag blockEntity, boolean waxed) {
		blockEntity.putBoolean("is_waxed", waxed);
	}
}
