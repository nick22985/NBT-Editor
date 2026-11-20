package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SkullUtil {
	public static final String PLAYER_HEAD = "minecraft:player_head";
	public static final String PLAYER_WALL_HEAD = "minecraft:player_wall_head";
	public static final String UNBOUND_NAME = "Unbound_Player";
	public static final String TEXTURES = "textures";
	private static final String LEGACY_KEY = "SkullOwner";
	private static final String KEY = "profile";

	private SkullUtil() {
	}

	public static boolean isPlayerHead(String id) {
		return PLAYER_HEAD.equals(id) || PLAYER_WALL_HEAD.equals(id);
	}

	private static String key(GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version) ? KEY : LEGACY_KEY;
	}

	public static Tag profile(CompoundTag root, boolean item, GameVersion version) {
		if (!item) {
			CompoundTag nbt = root.getCompound(BlockDocUtil.NBT);
			return nbt == null ? null : nbt.get(key(version));
		}
		return Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(root, KEY) : ItemDataUtil.dataTag(root, version, LEGACY_KEY);
	}

	public static void setProfile(CompoundTag root, boolean item, GameVersion version, Tag profile) {
		if (!item) {
			CompoundTag nbt = root.getOrCreateCompound(BlockDocUtil.NBT);
			if (profile == null) {
				nbt.remove(key(version));
			} else {
				nbt.put(key(version), profile);
			}
		} else if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(root, KEY, profile);
		} else {
			ItemDataUtil.putData(root, version, LEGACY_KEY, profile);
		}
	}

	public static String name(Tag profile) {
		if (profile instanceof StringTag) {
			return ((StringTag) profile).value();
		}
		if (profile instanceof CompoundTag) {
			CompoundTag tag = (CompoundTag) profile;
			String name = tag.getString("name");
			return name == null ? tag.getString("Name") : name;
		}
		return null;
	}

	public static UUID uuid(Tag profile) {
		if (!(profile instanceof CompoundTag)) {
			return null;
		}
		CompoundTag tag = (CompoundTag) profile;
		return UuidUtil.fromTag(tag.contains("id") ? tag.get("id") : tag.get("Id"));
	}

	public static Tag properties(Tag profile) {
		if (!(profile instanceof CompoundTag)) {
			return null;
		}
		CompoundTag tag = (CompoundTag) profile;
		Tag properties = tag.contains("properties") ? tag.get("properties") : tag.get("Properties");
		return hasTexture(properties) ? properties : null;
	}

	public static String texture(Tag profile) {
		return textureIn(properties(profile));
	}

	private static boolean hasTexture(Tag properties) {
		return textureIn(properties) != null;
	}

	private static String textureIn(Tag properties) {
		if (properties instanceof ListTag) {
			for (Tag entry : (ListTag) properties) {
				if (entry instanceof CompoundTag && TEXTURES.equals(((CompoundTag) entry).getString("name"))) {
					return ((CompoundTag) entry).getString("value");
				}
			}
		} else if (properties instanceof CompoundTag) {
			ListTag textures = ((CompoundTag) properties).getList(TEXTURES);
			if (textures != null && textures.size() > 0) {
				Tag first = textures.get(0);
				if (first instanceof CompoundTag) {
					return ((CompoundTag) first).getString("Value");
				}
				if (first instanceof StringTag) {
					return ((StringTag) first).value();
				}
			}
		}
		return null;
	}

	public static String skinId(String value) {
		try {
			Object root = JsonUtil.parse(new String(Base64.getMimeDecoder().decode(value), StandardCharsets.UTF_8), true);
			Object textures = root instanceof Map ? ((Map<?, ?>) root).get(TEXTURES) : null;
			Object skin = textures instanceof Map ? ((Map<?, ?>) textures).get("SKIN") : null;
			Object url = skin instanceof Map ? ((Map<?, ?>) skin).get("url") : null;
			if (url instanceof String) {
				String text = (String) url;
				return text.substring(text.lastIndexOf('/') + 1);
			}
		} catch (IllegalArgumentException | JsonException e) {
			return value;
		}
		return value;
	}

	public static Tag texturedProfile(UUID uuid, String value, GameVersion version) {
		boolean modern = Feature.ITEM_COMPONENTS.in(version);
		CompoundTag profile = new CompoundTag();
		profile.put(modern ? "id" : "Id", UuidUtil.toIntArray(uuid));
		CompoundTag texture = new CompoundTag();
		if (modern) {
			texture.putString("name", TEXTURES);
			texture.putString("value", value);
			profile.put("properties", new ListTag(List.of(texture)));
		} else {
			texture.putString("Value", value);
			CompoundTag properties = new CompoundTag();
			properties.put(TEXTURES, new ListTag(List.of(texture)));
			profile.put("Properties", properties);
		}
		return profile;
	}

	public static boolean unbind(CompoundTag root, boolean item, GameVersion version) {
		Tag profile = profile(root, item, version);
		Tag properties = properties(profile);
		if (properties == null) {
			return false;
		}
		boolean modern = Feature.ITEM_COMPONENTS.in(version);
		CompoundTag unbound = new CompoundTag();
		unbound.put(modern ? "id" : "Id", UuidUtil.toIntArray(new UUID(0L, 0L)));
		unbound.putString(modern ? "name" : "Name", UNBOUND_NAME);
		unbound.put(modern ? "properties" : "Properties", properties.copy());
		String owner = name(profile);
		setProfile(root, item, version, unbound);
		if (item && owner != null && !ItemFields.CUSTOM_NAME.has(root, version)) {
			ItemFields.CUSTOM_NAME.write(root, version, Text.translatable("block.minecraft.player_head.named", Text.literal(owner)).withStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW)));
		}
		return true;
	}
}
