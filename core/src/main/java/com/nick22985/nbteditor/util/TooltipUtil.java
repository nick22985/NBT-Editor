package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TooltipUtil {
	public static final String TOOLTIP_DISPLAY = "tooltip_display";
	public static final String HIDE_TOOLTIP = "hide_tooltip";
	public static final String HIDE_ADDITIONAL = "hide_additional_tooltip";
	public static final List<String> SHOW_IN_TOOLTIP = List.of("enchantments", "stored_enchantments", "attribute_modifiers", "unbreakable", "can_break", "can_place_on", "dyed_color", "trim", "jukebox_playable");
	public static final List<String> COMMON = List.of("enchantments", "stored_enchantments", "attribute_modifiers", "unbreakable", "can_break", "can_place_on", "dyed_color", "trim", "jukebox_playable", "lore", "potion_contents", "written_book_content", "firework_explosion", "fireworks", "banner_patterns", "container", "bundle_contents", "charged_projectiles", "map_id", "instrument", "ominous_bottle_amplifier", "suspicious_stew_effects", "damage", "block_state");

	private TooltipUtil() {
	}

	public static int hideFlags(CompoundTag item, GameVersion version) {
		Tag tag = ItemDataUtil.dataTag(item, version, "HideFlags");
		return tag instanceof NumericTag ? ((NumericTag) tag).intValue() : 0;
	}

	public static void hideFlags(CompoundTag item, GameVersion version, int flags) {
		ItemDataUtil.putData(item, version, "HideFlags", flags == 0 ? null : IntTag.of(flags));
	}

	public static boolean marker(CompoundTag item, String component) {
		return ItemStackUtil.component(item, component) != null && !ItemStackUtil.isComponentRemoved(item, component);
	}

	public static void marker(CompoundTag item, String component, boolean value) {
		ItemStackUtil.setComponent(item, component, value ? new CompoundTag() : null);
	}

	public static boolean shownInTooltip(CompoundTag item, String component) {
		Tag tag = ItemStackUtil.component(item, component);
		return !(tag instanceof CompoundTag) || ((CompoundTag) tag).getBoolean("show_in_tooltip", true);
	}

	public static void showInTooltip(CompoundTag item, String component, boolean shown) {
		Tag tag = ItemStackUtil.component(item, component);
		if (tag == null || ItemStackUtil.isComponentRemoved(item, component)) {
			return;
		}
		CompoundTag compound = wrap(component, tag);
		if (shown) {
			compound.remove("show_in_tooltip");
		} else {
			compound.put("show_in_tooltip", ByteTag.of(false));
		}
		ItemStackUtil.setComponent(item, component, compound);
	}

	private static CompoundTag wrap(String component, Tag tag) {
		switch (component) {
			case "enchantments":
			case "stored_enchantments":
				if (tag instanceof CompoundTag && ((CompoundTag) tag).contains("levels")) {
					return (CompoundTag) tag.copy();
				}
				return new CompoundTag().put("levels", tag.copy());
			case "attribute_modifiers":
				if (tag instanceof CompoundTag) {
					return (CompoundTag) tag.copy();
				}
				return new CompoundTag().put("modifiers", tag.copy());
			case "can_break":
			case "can_place_on":
				if (tag instanceof CompoundTag && ((CompoundTag) tag).contains("predicates")) {
					return (CompoundTag) tag.copy();
				}
				return new CompoundTag().put("predicates", tag instanceof ListTag ? tag.copy() : ListTag.of(tag.copy()));
			case "dyed_color":
				if (tag instanceof NumericTag) {
					return new CompoundTag().put("rgb", IntTag.of(((NumericTag) tag).intValue()));
				}
				return tag instanceof CompoundTag ? (CompoundTag) tag.copy() : new CompoundTag();
			case "jukebox_playable":
				if (tag instanceof StringTag) {
					return new CompoundTag().put("song", tag.copy());
				}
				return tag instanceof CompoundTag ? (CompoundTag) tag.copy() : new CompoundTag();
			default:
				return tag instanceof CompoundTag ? (CompoundTag) tag.copy() : new CompoundTag();
		}
	}

	public static boolean hideTooltip(CompoundTag item, GameVersion version) {
		if (Feature.TOOLTIP_DISPLAY.in(version)) {
			Tag tag = ItemStackUtil.component(item, TOOLTIP_DISPLAY);
			return tag instanceof CompoundTag && ((CompoundTag) tag).getBoolean(HIDE_TOOLTIP, false);
		}
		return marker(item, HIDE_TOOLTIP);
	}

	public static void hideTooltip(CompoundTag item, GameVersion version, boolean hide) {
		if (!Feature.TOOLTIP_DISPLAY.in(version)) {
			marker(item, HIDE_TOOLTIP, hide);
			return;
		}
		CompoundTag display = display(item);
		if (hide) {
			display.put(HIDE_TOOLTIP, ByteTag.of(true));
		} else {
			display.remove(HIDE_TOOLTIP);
		}
		putDisplay(item, display);
	}

	public static Set<String> hiddenComponents(CompoundTag item) {
		Set<String> hidden = new LinkedHashSet<>();
		ListTag list = display(item).getList("hidden_components");
		if (list != null) {
			for (Tag element : list) {
				if (element instanceof StringTag) {
					hidden.add(ItemStackUtil.namespaced(((StringTag) element).value()));
				}
			}
		}
		return hidden;
	}

	public static void hiddenComponents(CompoundTag item, Set<String> hidden) {
		CompoundTag display = display(item);
		if (hidden.isEmpty()) {
			display.remove("hidden_components");
		} else {
			display.put("hidden_components", ListTag.ofStrings(hidden));
		}
		putDisplay(item, display);
	}

	public static List<String> presentComponents(CompoundTag item) {
		List<String> present = new ArrayList<>();
		CompoundTag components = item.getCompound("components");
		if (components == null) {
			return present;
		}
		for (Map.Entry<String, Tag> entry : components.entries()) {
			String key = entry.getKey();
			if (!key.startsWith("!")) {
				present.add(ItemStackUtil.namespaced(key));
			}
		}
		return present;
	}

	private static CompoundTag display(CompoundTag item) {
		Tag tag = ItemStackUtil.component(item, TOOLTIP_DISPLAY);
		return tag instanceof CompoundTag ? (CompoundTag) tag.copy() : new CompoundTag();
	}

	private static void putDisplay(CompoundTag item, CompoundTag display) {
		ItemStackUtil.setComponent(item, TOOLTIP_DISPLAY, display.isEmpty() ? null : display);
	}
}
