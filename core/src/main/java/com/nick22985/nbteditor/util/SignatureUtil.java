package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SignatureUtil {
	private SignatureUtil() {
	}

	private static List<Text> lore(CompoundTag item, GameVersion version) {
		List<Text> lore = ItemFields.LORE.get(item, version);
		return lore == null ? new ArrayList<>() : new ArrayList<>(lore);
	}

	public static boolean has(CompoundTag item, GameVersion version, Text signature) {
		List<Text> lore = lore(item, version);
		return !lore.isEmpty() && lore.get(lore.size() - 1).plain().equals(signature.plain());
	}

	public static boolean add(CompoundTag item, GameVersion version, Text signature) {
		if (has(item, version, signature)) {
			return false;
		}
		List<Text> lore = lore(item, version);
		lore.add(signature);
		ItemFields.LORE.write(item, version, lore);
		return true;
	}

	public static boolean remove(CompoundTag item, GameVersion version, Text signature) {
		if (!has(item, version, signature)) {
			return false;
		}
		List<Text> lore = lore(item, version);
		lore.remove(lore.size() - 1);
		ItemFields.LORE.write(item, version, lore.isEmpty() ? null : lore);
		return true;
	}

	public static boolean replace(CompoundTag item, GameVersion version, Text previous, Text signature) {
		if (!has(item, version, previous)) {
			return false;
		}
		List<Text> lore = lore(item, version);
		lore.set(lore.size() - 1, signature);
		ItemFields.LORE.write(item, version, lore);
		return true;
	}
}
