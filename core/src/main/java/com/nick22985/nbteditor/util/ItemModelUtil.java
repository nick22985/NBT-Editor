package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.data.ModelOption;
import com.nick22985.nbteditor.data.PackModel;
import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.platform.ResourceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemModelUtil {
	public static final String ITEM_MODEL = "item_model";
	public static final String FALLBACK_ITEM = "minecraft:paper";
	private static final String CUSTOM_MODEL_DATA = "custom_model_data";
	private static final String VANILLA = "minecraft:";
	private static final String JSON = ".json";
	private static final String FILE_PACK = "file/";
	private static final String ZIP = ".zip";

	private ItemModelUtil() {
	}

	public static List<ModelOption> customModelData(ResourceService resources, String model, GameVersion version) {
		String id = ItemStackUtil.namespaced(model);
		int colon = id.indexOf(':');
		String namespace = id.substring(0, colon);
		String path = id.substring(colon + 1);
		if (Feature.CUSTOM_MODEL_DATA_LISTS.in(version)) {
			String json = resources.read(namespace + ":items/" + path + JSON);
			return json == null ? List.of() : definitionOptions(json);
		}
		String json = resources.read(namespace + ":models/item/" + path + JSON);
		return json == null ? List.of() : overrideOptions(json);
	}

	public static List<String> itemModels(ResourceService resources, GameVersion version) {
		String folder = Feature.CUSTOM_MODEL_DATA_LISTS.in(version) ? "items" : "models/item";
		List<String> out = new ArrayList<>();
		for (String resource : resources.list(folder)) {
			int colon = resource.indexOf(':');
			String path = resource.substring(colon + 1);
			if (path.startsWith(folder + "/") && path.endsWith(JSON)) {
				out.add(resource.substring(0, colon + 1) + path.substring(folder.length() + 1, path.length() - JSON.length()));
			}
		}
		out.sort(null);
		return out;
	}

	public static List<PackModel> catalog(ResourceService resources, GameVersion version, Predicate<String> isItem) {
		List<PackModel> out = new ArrayList<>();
		for (ModelEntry entry : entries(resources, version, isItem)) {
			out.add(entry.model());
		}
		return out;
	}

	public static List<ModelEntry> entries(ResourceService resources, GameVersion version, Predicate<String> isItem) {
		boolean components = Feature.ITEM_MODEL_COMPONENT.in(version);
		boolean definitions = Feature.CUSTOM_MODEL_DATA_LISTS.in(version);
		List<ModelEntry> out = new ArrayList<>();
		for (String id : itemModels(resources, version)) {
			boolean item = isItem.test(id);
			if (!item && !components) {
				continue;
			}
			String base = item ? id : FALLBACK_ITEM;
			String itemModel = item ? null : id;
			String pack = packTitle(resources.pack(source(id, version)));
			if (!item && (definitions || !id.startsWith(VANILLA))) {
				add(out, new PackModel(base, itemModel, null, ""), pack);
			}
			for (ModelOption option : customModelData(resources, id, version)) {
				add(out, new PackModel(base, itemModel, option.value(), option.model()), pack);
			}
		}
		return out;
	}

	private static void add(List<ModelEntry> out, PackModel model, String pack) {
		out.add(new ModelEntry(model, pack, folder(model), out.size()));
	}

	private static String source(String id, GameVersion version) {
		int colon = id.indexOf(':');
		String folder = Feature.CUSTOM_MODEL_DATA_LISTS.in(version) ? "items/" : "models/item/";
		return id.substring(0, colon + 1) + folder + id.substring(colon + 1) + JSON;
	}

	public static String folder(PackModel model) {
		String id = model.itemModel() != null ? model.itemModel() : model.model();
		if (id == null || id.isEmpty()) {
			return "";
		}
		String path = id.substring(id.indexOf(':') + 1);
		if (model.itemModel() == null) {
			path = path.replaceFirst("^(item|block)s?/", "");
		}
		int cut = path.lastIndexOf('/');
		return cut < 0 ? "" : path.substring(0, cut);
	}

	public static String packTitle(String pack) {
		if (pack == null) {
			return "";
		}
		String title = pack.startsWith(FILE_PACK) ? pack.substring(FILE_PACK.length()) : pack;
		if (title.toLowerCase(Locale.ROOT).endsWith(ZIP)) {
			title = title.substring(0, title.length() - ZIP.length());
		}
		return title.trim();
	}

	public static CompoundTag item(PackModel model, GameVersion version) {
		CompoundTag item = ItemStackUtil.create(model.item(), 1, version);
		if (model.itemModel() != null) {
			ItemStackUtil.setComponent(item, ITEM_MODEL, StringTag.of(model.itemModel()));
		}
		if (model.value() != null) {
			ItemFields.CUSTOM_MODEL_DATA.write(item, version, model.value());
		}
		return item;
	}

	public static List<ModelOption> overrideOptions(String json) {
		Map<Integer, String> found = new TreeMap<>();
		Map<String, Object> root = parse(json);
		if (root == null || !(root.get("overrides") instanceof List)) {
			return List.of();
		}
		for (Object entry : (List<?>) root.get("overrides")) {
			if (!(entry instanceof Map)) {
				continue;
			}
			Object predicate = ((Map<?, ?>) entry).get("predicate");
			Object value = predicate instanceof Map ? ((Map<?, ?>) predicate).get(CUSTOM_MODEL_DATA) : null;
			if (value instanceof Number) {
				Object model = ((Map<?, ?>) entry).get("model");
				found.put(threshold((Number) value), model instanceof String ? (String) model : "");
			}
		}
		return options(found);
	}

	public static List<ModelOption> definitionOptions(String json) {
		Map<Integer, String> found = new TreeMap<>();
		Map<String, Object> root = parse(json);
		if (root != null) {
			walk(root.get("model"), found);
		}
		return options(found);
	}

	private static void walk(Object node, Map<Integer, String> found) {
		if (!(node instanceof Map)) {
			return;
		}
		Map<?, ?> map = (Map<?, ?>) node;
		String type = bare(map.get("type"));
		if ("range_dispatch".equals(type) && CUSTOM_MODEL_DATA.equals(bare(map.get("property"))) && index(map) == 0 && map.get("entries") instanceof List) {
			for (Object entry : (List<?>) map.get("entries")) {
				if (entry instanceof Map && ((Map<?, ?>) entry).get("threshold") instanceof Number) {
					Object model = ((Map<?, ?>) entry).get("model");
					found.putIfAbsent(threshold((Number) ((Map<?, ?>) entry).get("threshold")), describe(model));
				}
			}
		}
		for (Object value : map.values()) {
			if (value instanceof Map) {
				walk(value, found);
			} else if (value instanceof List) {
				for (Object element : (List<?>) value) {
					walk(element, found);
				}
			}
		}
	}

	private static String describe(Object node) {
		if (!(node instanceof Map)) {
			return "";
		}
		Map<?, ?> map = (Map<?, ?>) node;
		if ("model".equals(bare(map.get("type"))) && map.get("model") instanceof String) {
			return (String) map.get("model");
		}
		String type = bare(map.get("type"));
		return type == null ? "" : type;
	}

	private static int index(Map<?, ?> map) {
		Object index = map.get("index");
		return index instanceof Number ? ((Number) index).intValue() : 0;
	}

	private static String bare(Object value) {
		if (!(value instanceof String)) {
			return null;
		}
		return ItemStackUtil.stripNamespace(((String) value).toLowerCase(Locale.ROOT));
	}

	private static int threshold(Number value) {
		return (int) Math.ceil(value.doubleValue());
	}

	private static List<ModelOption> options(Map<Integer, String> found) {
		List<ModelOption> out = new ArrayList<>();
		for (Map.Entry<Integer, String> entry : found.entrySet()) {
			out.add(new ModelOption(entry.getKey(), entry.getValue()));
		}
		return out;
	}

	private static Map<String, Object> parse(String json) {
		try {
			return JsonUtil.parseObject(json);
		} catch (JsonException e) {
			return null;
		}
	}
}
