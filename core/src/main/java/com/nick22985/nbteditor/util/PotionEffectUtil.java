package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.PotionEffect;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotionEffectUtil {
	private static final List<String> LEGACY_IDS = List.of(
		"", "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage", "jump_boost",
		"nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility", "blindness", "night_vision",
		"hunger", "weakness", "poison", "wither", "health_boost", "absorption", "saturation", "glowing", "levitation", "luck",
		"unluck", "slow_falling", "conduit_power", "dolphins_grace", "bad_omen", "hero_of_the_village", "darkness"
	);
	private static final Set<String> MODERN_KEYS = Set.of("id", "amplifier", "duration", "ambient", "show_particles", "show_icon");
	private static final Set<String> LEGACY_KEYS = Set.of("Id", "Amplifier", "Duration", "Ambient", "ShowParticles", "ShowIcon");

	private PotionEffectUtil() {
	}

	public static String fromLegacyId(int id) {
		return id > 0 && id < LEGACY_IDS.size() ? "minecraft:" + LEGACY_IDS.get(id) : "minecraft:unknown_" + id;
	}

	public static int toLegacyId(String id) {
		String path = ItemStackUtil.stripNamespace(id);
		int index = LEGACY_IDS.indexOf(path);
		if (index > 0) {
			return index;
		}
		if (path.startsWith("unknown_")) {
			try {
				return Integer.parseInt(path.substring("unknown_".length()));
			} catch (NumberFormatException e) {
				return 1;
			}
		}
		return 1;
	}

	private static String readId(Tag tag) {
		if (tag instanceof StringTag) {
			return ItemStackUtil.namespaced(((StringTag) tag).value());
		}
		if (tag instanceof NumericTag) {
			return fromLegacyId(((NumericTag) tag).intValue());
		}
		return null;
	}

	public static PotionEffect read(CompoundTag tag, GameVersion version) {
		boolean modern = Feature.STRING_EFFECT_IDS.in(version);
		String id = readId(tag.get(modern ? "id" : "Id"));
		if (id == null) {
			return null;
		}
		CompoundTag extra = new CompoundTag();
		Set<String> known = modern ? MODERN_KEYS : LEGACY_KEYS;
		for (String key : tag.keys()) {
			if (!known.contains(key)) {
				extra.put(key, tag.get(key).copy());
			}
		}
		if (modern) {
			boolean particles = tag.getBoolean("show_particles", true);
			return new PotionEffect(id, tag.getInt("amplifier", 0), tag.getInt("duration", 0), tag.getBoolean("ambient", false), particles, tag.getBoolean("show_icon", particles), extra);
		}
		boolean particles = tag.getBoolean("ShowParticles", true);
		return new PotionEffect(id, tag.getInt("Amplifier", 0), tag.getInt("Duration", 0), tag.getBoolean("Ambient", false), particles, tag.getBoolean("ShowIcon", particles), extra);
	}

	public static CompoundTag write(PotionEffect effect, GameVersion version) {
		CompoundTag tag = effect.extra() == null ? new CompoundTag() : effect.extra().copy();
		if (Feature.STRING_EFFECT_IDS.in(version)) {
			tag.putString("id", effect.id());
			tag.putByte("amplifier", (byte) effect.amplifier());
			tag.putInt("duration", effect.duration());
			if (effect.ambient()) {
				tag.putBoolean("ambient", true);
			}
			if (!effect.showParticles()) {
				tag.putBoolean("show_particles", false);
			}
			if (effect.showIcon() != effect.showParticles()) {
				tag.putBoolean("show_icon", effect.showIcon());
			}
			return tag;
		}
		tag.put("Id", ByteTag.of((byte) toLegacyId(effect.id())));
		tag.putByte("Amplifier", (byte) effect.amplifier());
		tag.putInt("Duration", effect.duration());
		tag.putBoolean("Ambient", effect.ambient());
		tag.putBoolean("ShowParticles", effect.showParticles());
		tag.putBoolean("ShowIcon", effect.showIcon());
		return tag;
	}

	public static PotionEffect readStew(CompoundTag tag, GameVersion version) {
		boolean modern = Feature.STRING_EFFECT_IDS.in(version);
		String id = readId(tag.get(modern ? "id" : "EffectId"));
		if (id == null) {
			return null;
		}
		int duration = tag.getInt(modern ? "duration" : "EffectDuration", 160);
		return new PotionEffect(id, 0, duration, false, true, true, new CompoundTag());
	}

	public static CompoundTag writeStew(PotionEffect effect, GameVersion version) {
		CompoundTag tag = new CompoundTag();
		if (Feature.STRING_EFFECT_IDS.in(version)) {
			tag.putString("id", effect.id());
			tag.putInt("duration", effect.duration());
		} else {
			tag.put("EffectId", ByteTag.of((byte) toLegacyId(effect.id())));
			tag.putInt("EffectDuration", effect.duration());
		}
		return tag;
	}

	public static Integer parseDuration(String text, boolean allowInfinite) {
		String value = text.trim().toLowerCase(Locale.ROOT);
		if (value.isEmpty()) {
			return null;
		}
		if (value.equals("∞") || value.equals("inf") || value.equals("infinite") || value.equals("-1")) {
			return allowInfinite ? PotionEffect.INFINITE : null;
		}
		try {
			if (value.endsWith("t")) {
				int ticks = Integer.parseInt(value.substring(0, value.length() - 1).trim());
				return ticks < 0 ? null : ticks;
			}
			int colon = value.indexOf(':');
			double seconds = colon < 0 ? Double.parseDouble(value) : Integer.parseInt(value.substring(0, colon)) * 60.0 + Double.parseDouble(value.substring(colon + 1));
			if (seconds < 0 || seconds * 20 > Integer.MAX_VALUE || Double.isNaN(seconds)) {
				return null;
			}
			return (int) Math.round(seconds * 20);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String formatDuration(int ticks) {
		if (ticks == PotionEffect.INFINITE) {
			return "∞";
		}
		if (ticks % 20 != 0) {
			return ticks + "t";
		}
		int seconds = ticks / 20;
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}
}
