package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.json.JsonNull;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.SnbtWriter;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.nbt.TagType;
import com.nick22985.nbteditor.text.ClickAction;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.Content;
import com.nick22985.nbteditor.text.HoverAction;
import com.nick22985.nbteditor.text.HoverEvent;
import com.nick22985.nbteditor.text.KeybindContent;
import com.nick22985.nbteditor.text.LiteralContent;
import com.nick22985.nbteditor.text.NbtContent;
import com.nick22985.nbteditor.text.RawContent;
import com.nick22985.nbteditor.text.ScoreContent;
import com.nick22985.nbteditor.text.SelectorContent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.text.TranslatableContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextCodecUtil {
	private static final Set<String> RAW_TAG_KEYS = Set.of("components", "tag");
	private static final Set<String> STYLE_KEYS = Set.of("color", "shadow_color", "bold", "italic", "underlined", "strikethrough", "obfuscated", "font", "insertion", "clickEvent", "click_event", "hoverEvent", "hover_event", "extra", "type");

	private TextCodecUtil() {
	}

	public static String toJson(Text text, TextDialect dialect) {
		return JsonUtil.write(jsonSafe(toTree(text, dialect)));
	}

	@SuppressWarnings("unchecked")
	private static Object jsonSafe(Object tree) {
		if (tree instanceof Tag) {
			return TagTreeUtil.toTree((Tag) tree);
		}
		if (tree instanceof Map) {
			Map<String, Object> map = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) tree).entrySet()) {
				map.put(entry.getKey(), jsonSafe(entry.getValue()));
			}
			return map;
		}
		if (tree instanceof List) {
			List<Object> list = new ArrayList<>();
			for (Object element : (List<Object>) tree) {
				list.add(jsonSafe(element));
			}
			return list;
		}
		return tree;
	}

	public static Text fromJson(String json) {
		try {
			return fromTree(JsonUtil.parse(json, true));
		} catch (JsonException e) {
			return Text.literal(json);
		}
	}

	public static Text fromJsonStrict(String json) throws JsonException {
		return fromTree(JsonUtil.parse(json, false));
	}

	public static Tag toNbt(Text text) {
		return TagTreeUtil.fromTree(toTree(text, TextDialect.NBT));
	}

	public static Text fromNbt(Tag tag) {
		return fromTree(nbtToTree(tag));
	}

	public static Tag toStored(Text text, TextDialect dialect) {
		return dialect.storesAsNbt() ? toNbt(text) : StringTag.of(toJson(text, dialect));
	}

	public static Text fromStored(Tag tag, TextDialect dialect) {
		if (tag == null) {
			return null;
		}
		if (tag instanceof StringTag && !dialect.storesAsNbt()) {
			return fromJson(((StringTag) tag).value());
		}
		return fromNbt(tag);
	}

	public static Text fromAny(Tag tag) {
		if (tag instanceof StringTag) {
			String value = ((StringTag) tag).value();
			String trimmed = value.trim();
			if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
				try {
					return fromTree(JsonUtil.parse(trimmed, false));
				} catch (JsonException ignored) {
				}
			}
			return Text.literal(value);
		}
		return fromNbt(tag);
	}

	public static Object toTree(Text text, TextDialect dialect) {
		if (text.style().isEmpty() && text.siblings().isEmpty() && text.content() instanceof LiteralContent) {
			return ((LiteralContent) text.content()).text();
		}
		Map<String, Object> map = new LinkedHashMap<>();
		writeContent(text.content(), map, dialect);
		writeStyle(text.style(), map, dialect);
		if (!text.siblings().isEmpty()) {
			List<Object> extra = new ArrayList<>();
			for (Text sibling : text.siblings()) {
				extra.add(toTree(sibling, dialect));
			}
			map.put("extra", extra);
		}
		return map;
	}

	private static void writeContent(Content content, Map<String, Object> map, TextDialect dialect) {
		if (content instanceof LiteralContent) {
			map.put("text", ((LiteralContent) content).text());
		} else if (content instanceof TranslatableContent) {
			TranslatableContent translatable = (TranslatableContent) content;
			map.put("translate", translatable.key());
			if (translatable.fallback() != null) {
				map.put("fallback", translatable.fallback());
			}
			if (!translatable.args().isEmpty()) {
				List<Object> with = new ArrayList<>();
				for (Object arg : translatable.args()) {
					with.add(arg instanceof Text ? toTree((Text) arg, dialect) : arg);
				}
				map.put("with", with);
			}
		} else if (content instanceof KeybindContent) {
			map.put("keybind", ((KeybindContent) content).key());
		} else if (content instanceof ScoreContent) {
			Map<String, Object> score = new LinkedHashMap<>();
			score.put("name", ((ScoreContent) content).name());
			score.put("objective", ((ScoreContent) content).objective());
			map.put("score", score);
		} else if (content instanceof SelectorContent) {
			map.put("selector", ((SelectorContent) content).pattern());
			if (((SelectorContent) content).separator() != null) {
				map.put("separator", toTree(((SelectorContent) content).separator(), dialect));
			}
		} else if (content instanceof NbtContent) {
			NbtContent nbt = (NbtContent) content;
			map.put("nbt", nbt.path());
			if (nbt.interpret()) {
				map.put("interpret", true);
			}
			if (nbt.separator() != null) {
				map.put("separator", toTree(nbt.separator(), dialect));
			}
			map.put(nbt.sourceKind(), nbt.source());
		} else if (content instanceof RawContent) {
			map.putAll(((RawContent) content).fields());
		}
	}

	private static void writeStyle(Style style, Map<String, Object> map, TextDialect dialect) {
		if (style.color() != null) {
			map.put("color", style.color().serialize());
		}
		if (style.shadowColor() != null) {
			map.put("shadow_color", style.shadowColor());
		}
		putFlag(map, "bold", style.bold());
		putFlag(map, "italic", style.italic());
		putFlag(map, "underlined", style.underlined());
		putFlag(map, "strikethrough", style.strikethrough());
		putFlag(map, "obfuscated", style.obfuscated());
		if (style.font() != null) {
			map.put("font", style.font());
		}
		if (style.insertion() != null) {
			map.put("insertion", style.insertion());
		}
		if (style.clickEvent() != null) {
			writeClick(style.clickEvent(), map, dialect);
		}
		if (style.hoverEvent() != null && !(dialect.componentItems() && style.hoverEvent().isEmptyItem())) {
			writeHover(style.hoverEvent(), map, dialect);
		}
	}

	private static void putFlag(Map<String, Object> map, String key, Boolean value) {
		if (value != null) {
			map.put(key, value);
		}
	}

	private static void writeClick(ClickEvent event, Map<String, Object> map, TextDialect dialect) {
		Map<String, Object> click = new LinkedHashMap<>();
		click.put("action", event.action().serializedName());
		if (!dialect.snakeCaseEvents()) {
			click.put("value", event.value());
			map.put("clickEvent", click);
			return;
		}
		switch (event.action()) {
			case OPEN_URL:
				click.put("url", event.value());
				break;
			case OPEN_FILE:
				click.put("path", event.value());
				break;
			case RUN_COMMAND:
			case SUGGEST_COMMAND:
				click.put("command", event.value());
				break;
			case CHANGE_PAGE:
				try {
					click.put("page", Integer.parseInt(event.value().trim()));
				} catch (NumberFormatException e) {
					click.put("page", 1);
				}
				break;
			case SHOW_DIALOG:
				click.put("dialog", event.value());
				break;
			case CUSTOM:
				click.put("id", event.value());
				if (event.payload() != null) {
					click.put("payload", event.payload());
				}
				break;
			default:
				click.put("value", event.value());
				break;
		}
		map.put("click_event", click);
	}

	private static void writeHover(HoverEvent event, Map<String, Object> map, TextDialect dialect) {
		Map<String, Object> hover = new LinkedHashMap<>();
		hover.put("action", event.action().serializedName());
		if (dialect.snakeCaseEvents()) {
			switch (event.action()) {
				case SHOW_TEXT:
					hover.put("value", toTree(event.text(), dialect));
					break;
				case SHOW_ITEM:
					hover.put("id", event.id());
					if (event.count() != 1) {
						hover.put("count", event.count());
					}
					if (event.data() != null && !event.data().isEmpty()) {
						hover.put("components", event.data().copy());
					}
					break;
				case SHOW_ENTITY:
					hover.put("id", event.id());
					hover.put("uuid", event.uuid().toString());
					if (event.text() != null) {
						hover.put("name", toTree(event.text(), dialect));
					}
					break;
			}
			map.put("hover_event", hover);
			return;
		}
		switch (event.action()) {
			case SHOW_TEXT:
				hover.put("contents", toTree(event.text(), dialect));
				break;
			case SHOW_ITEM: {
				Map<String, Object> contents = new LinkedHashMap<>();
				contents.put("id", event.id());
				if (event.count() != 1) {
					contents.put("count", event.count());
				}
				if (event.data() != null && !event.data().isEmpty()) {
					if (dialect.componentItems()) {
						contents.put("components", TagTreeUtil.toTree(event.data()));
					} else {
						contents.put("tag", SnbtWriter.compact(SnbtDialect.LEGACY).write(event.data()));
					}
				}
				hover.put("contents", contents);
				break;
			}
			case SHOW_ENTITY: {
				Map<String, Object> contents = new LinkedHashMap<>();
				contents.put("type", event.id());
				contents.put("id", event.uuid().toString());
				if (event.text() != null) {
					contents.put("name", toTree(event.text(), dialect));
				}
				hover.put("contents", contents);
				break;
			}
		}
		map.put("hoverEvent", hover);
	}

	@SuppressWarnings("unchecked")
	public static Text fromTree(Object tree) {
		if (tree instanceof String) {
			return Text.literal((String) tree);
		}
		if (tree instanceof Number || tree instanceof Boolean) {
			return Text.literal(String.valueOf(tree));
		}
		if (tree instanceof List) {
			List<Object> list = (List<Object>) tree;
			if (list.isEmpty()) {
				return Text.EMPTY;
			}
			Text first = fromTree(list.get(0));
			for (int i = 1; i < list.size(); i++) {
				first = first.append(fromTree(list.get(i)));
			}
			return first;
		}
		if (tree instanceof Tag) {
			return fromNbt((Tag) tree);
		}
		if (!(tree instanceof Map)) {
			return Text.EMPTY;
		}
		Map<String, Object> map = (Map<String, Object>) tree;
		Content content = readContent(map);
		Style style = readStyle(map);
		List<Text> siblings = new ArrayList<>();
		Object extra = map.get("extra");
		if (extra instanceof List) {
			for (Object element : (List<Object>) extra) {
				siblings.add(fromTree(element));
			}
		}
		return Text.of(content, style, siblings);
	}

	@SuppressWarnings("unchecked")
	private static Content readContent(Map<String, Object> map) {
		String type = string(map.get("type"));
		if ((type == null || type.equals("text")) && map.containsKey("text")) {
			return new LiteralContent(stringify(map.get("text")));
		}
		if ((type == null || type.equals("translatable")) && map.containsKey("translate")) {
			List<Object> args = new ArrayList<>();
			Object with = map.get("with");
			if (with instanceof List) {
				for (Object arg : (List<Object>) with) {
					args.add(arg instanceof String || arg instanceof Map || arg instanceof List ? fromTree(arg) : arg);
				}
			}
			return new TranslatableContent(stringify(map.get("translate")), string(map.get("fallback")), args);
		}
		if ((type == null || type.equals("score")) && map.get("score") instanceof Map) {
			Map<String, Object> score = (Map<String, Object>) map.get("score");
			return new ScoreContent(stringify(score.get("name")), stringify(score.get("objective")));
		}
		if ((type == null || type.equals("selector")) && map.containsKey("selector")) {
			return new SelectorContent(stringify(map.get("selector")), map.containsKey("separator") ? fromTree(map.get("separator")) : null);
		}
		if ((type == null || type.equals("keybind")) && map.containsKey("keybind")) {
			return new KeybindContent(stringify(map.get("keybind")));
		}
		if ((type == null || type.equals("nbt")) && map.containsKey("nbt")) {
			String kind = map.containsKey("block") ? "block" : map.containsKey("entity") ? "entity" : "storage";
			return new NbtContent(stringify(map.get("nbt")), bool(map.get("interpret")) == Boolean.TRUE, map.containsKey("separator") ? fromTree(map.get("separator")) : null, kind, stringify(map.get(kind)));
		}
		Map<String, Object> fields = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (!STYLE_KEYS.contains(entry.getKey())) {
				fields.put(entry.getKey(), entry.getValue());
			}
		}
		if (fields.isEmpty()) {
			return LiteralContent.EMPTY;
		}
		String kind = type != null ? type : map.containsKey("object") ? "object" : "unknown";
		if (type != null) {
			fields.put("type", type);
		}
		return new RawContent(kind, fields);
	}

	@SuppressWarnings("unchecked")
	private static Style readStyle(Map<String, Object> map) {
		Style style = Style.EMPTY;
		String color = string(map.get("color"));
		if (color != null) {
			style = style.withColor(TextColor.parse(color));
		}
		Object shadow = map.get("shadow_color");
		if (shadow instanceof Number) {
			style = style.withShadowColor(((Number) shadow).intValue());
		} else if (shadow instanceof List && ((List<Object>) shadow).size() == 4) {
			int argb = 0;
			List<Object> parts = (List<Object>) shadow;
			int[] order = {3, 0, 1, 2};
			for (int index : order) {
				Object part = parts.get(index);
				int channel = part instanceof Number ? Math.round(Math.max(0, Math.min(1, ((Number) part).floatValue())) * 255) : 0;
				argb = argb << 8 | channel;
			}
			style = style.withShadowColor(argb);
		}
		style = style.withBold(bool(map.get("bold")))
			.withItalic(bool(map.get("italic")))
			.withUnderlined(bool(map.get("underlined")))
			.withStrikethrough(bool(map.get("strikethrough")))
			.withObfuscated(bool(map.get("obfuscated")))
			.withFont(string(map.get("font")))
			.withInsertion(string(map.get("insertion")));
		Object click = map.containsKey("click_event") ? map.get("click_event") : map.get("clickEvent");
		if (click instanceof Map) {
			style = style.withClickEvent(readClick((Map<String, Object>) click));
		}
		Object hover = map.containsKey("hover_event") ? map.get("hover_event") : map.get("hoverEvent");
		if (hover instanceof Map) {
			style = style.withHoverEvent(readHover((Map<String, Object>) hover));
		}
		return style;
	}

	private static ClickEvent readClick(Map<String, Object> map) {
		ClickAction action = ClickAction.byName(string(map.get("action")));
		if (action == null) {
			return null;
		}
		String[] keys = {"value", "url", "command", "page", "path", "dialog", "id"};
		String value = "";
		for (String key : keys) {
			if (map.containsKey(key)) {
				Object raw = map.get(key);
				value = raw instanceof Map || raw instanceof Tag ? JsonUtil.write(raw instanceof Tag ? TagTreeUtil.toTree((Tag) raw) : raw) : stringify(raw);
				break;
			}
		}
		return new ClickEvent(action, value, map.get("payload"));
	}

	@SuppressWarnings("unchecked")
	private static HoverEvent readHover(Map<String, Object> map) {
		HoverAction action = HoverAction.byName(string(map.get("action")));
		if (action == null) {
			return null;
		}
		Object contents = map.containsKey("contents") ? map.get("contents") : null;
		switch (action) {
			case SHOW_TEXT:
				return HoverEvent.showText(fromTree(contents != null ? contents : map.get("value")));
			case SHOW_ITEM: {
				if (contents instanceof String) {
					return HoverEvent.showItem((String) contents, 1, null);
				}
				if (contents instanceof Map) {
					return readItem((Map<String, Object>) contents);
				}
				if (map.containsKey("id")) {
					return readItem(map);
				}
				Object value = map.get("value");
				if (value instanceof String) {
					try {
						CompoundTag item = SnbtUtil.parseCompound((String) value, SnbtDialect.LEGACY);
						return HoverEvent.showItem(item.getString("id", "minecraft:air"), item.getInt("Count", item.getInt("count", 1)), item.getCompound("tag"));
					} catch (SnbtException e) {
						return null;
					}
				}
				return null;
			}
			case SHOW_ENTITY: {
				Map<String, Object> source = contents instanceof Map ? (Map<String, Object>) contents : map;
				String entityType = contents instanceof Map ? string(source.get("type")) : string(source.get("id"));
				Object uuid = contents instanceof Map ? source.get("id") : source.get("uuid");
				UUID parsed = uuid(uuid);
				if (entityType == null || parsed == null) {
					return null;
				}
				return HoverEvent.showEntity(entityType, parsed, source.containsKey("name") ? fromTree(source.get("name")) : null);
			}
			default:
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static HoverEvent readItem(Map<String, Object> map) {
		String id = string(map.get("id"));
		if (id == null) {
			return null;
		}
		Object countValue = map.containsKey("count") ? map.get("count") : map.get("Count");
		int count = countValue instanceof Number ? ((Number) countValue).intValue() : 1;
		CompoundTag data = null;
		Object raw = map.containsKey("components") ? map.get("components") : map.get("tag");
		if (raw instanceof CompoundTag) {
			data = ((CompoundTag) raw).copy();
		} else if (raw instanceof Map) {
			data = (CompoundTag) TagTreeUtil.fromTree(raw);
		} else if (raw instanceof String) {
			try {
				data = SnbtUtil.parseCompound((String) raw, SnbtDialect.LEGACY);
			} catch (SnbtException ignored) {
			}
		}
		return HoverEvent.showItem(id, count, data);
	}

	@SuppressWarnings("unchecked")
	private static UUID uuid(Object value) {
		if (value instanceof String) {
			try {
				return UUID.fromString((String) value);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		if (value instanceof Tag) {
			return UuidUtil.fromTag((Tag) value);
		}
		if (value instanceof List && ((List<Object>) value).size() == 4) {
			int[] parts = new int[4];
			for (int i = 0; i < 4; i++) {
				Object part = ((List<Object>) value).get(i);
				if (!(part instanceof Number)) {
					return null;
				}
				parts[i] = ((Number) part).intValue();
			}
			return UuidUtil.fromIntArray(parts);
		}
		return null;
	}

	private static Object nbtToTree(Tag tag) {
		if (tag instanceof CompoundTag) {
			Map<String, Object> map = new LinkedHashMap<>();
			for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
				Tag value = entry.getValue();
				if (RAW_TAG_KEYS.contains(entry.getKey()) && value instanceof CompoundTag) {
					map.put(entry.getKey(), value);
				} else if (entry.getKey().equals("uuid") && value.type() == TagType.INT_ARRAY) {
					map.put(entry.getKey(), value);
				} else {
					map.put(entry.getKey(), nbtToTree(value));
				}
			}
			return map;
		}
		if (tag instanceof ListTag) {
			List<Object> list = new ArrayList<>();
			for (Tag element : (ListTag) tag) {
				list.add(nbtToTree(element));
			}
			return list;
		}
		return TagTreeUtil.toTree(tag);
	}

	private static String string(Object value) {
		return value instanceof String ? (String) value : null;
	}

	private static String stringify(Object value) {
		if (value == null || value == JsonNull.INSTANCE) {
			return "";
		}
		return String.valueOf(value);
	}

	private static Boolean bool(Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		if (value instanceof String) {
			if (value.equals("true")) {
				return true;
			}
			if (value.equals("false")) {
				return false;
			}
		}
		return null;
	}
}
