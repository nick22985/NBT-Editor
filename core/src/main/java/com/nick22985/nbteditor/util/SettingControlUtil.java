package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.config.BoolSettingType;
import com.nick22985.nbteditor.config.ColorSettingType;
import com.nick22985.nbteditor.config.DoubleSettingType;
import com.nick22985.nbteditor.config.EnumSettingType;
import com.nick22985.nbteditor.config.IntSettingType;
import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.config.SettingType;
import com.nick22985.nbteditor.config.StringListSettingType;
import com.nick22985.nbteditor.config.StringMapSettingType;
import com.nick22985.nbteditor.config.StringSettingType;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.CycleButton;
import com.nick22985.nbteditor.ui.widget.NumberField;
import com.nick22985.nbteditor.ui.widget.TextField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SettingControlUtil {
	private SettingControlUtil() {
	}

	@SuppressWarnings("unchecked")
	public static Widget control(Setting<?> setting) {
		SettingType<?> type = setting.type();
		if (type instanceof BoolSettingType) {
			Setting<Boolean> bool = (Setting<Boolean>) setting;
			return new Checkbox(Text.EMPTY, bool.get(), bool::set);
		}
		if (type instanceof IntSettingType range) {
			Setting<Integer> integer = (Setting<Integer>) setting;
			return new NumberField(true, integer.get()).range(range.min(), range.max()).onValue(value -> integer.set((int) value));
		}
		if (type instanceof DoubleSettingType range) {
			Setting<Double> decimal = (Setting<Double>) setting;
			return new NumberField(false, decimal.get()).range(range.min(), range.max()).step(0.1).onValue(decimal::set);
		}
		if (type instanceof ColorSettingType) {
			Setting<Integer> color = (Setting<Integer>) setting;
			return new TextField(ColorUtil.toHex(color.get()))
					.maxLength(7)
					.validator(text -> ColorUtil.parseHex(text) == null ? Text.translatable("nbtedit.ui.color.invalid") : null)
					.onChange(text -> {
						Integer parsed = ColorUtil.parseHex(text);
						if (parsed != null) {
							color.set(parsed);
						}
					});
		}
		if (type instanceof EnumSettingType<?> enumType) {
			return enumControl(setting, enumType);
		}
		if (type instanceof StringSettingType) {
			Setting<String> string = (Setting<String>) setting;
			return new TextField(string.get()).onChange(string::set);
		}
		if (type instanceof StringListSettingType) {
			Setting<List<String>> list = (Setting<List<String>>) setting;
			return new TextField(String.join(", ", list.get())).onChange(text -> list.set(splitList(text)));
		}
		if (type instanceof StringMapSettingType) {
			Setting<Map<String, String>> map = (Setting<Map<String, String>>) setting;
			return new TextField(joinMap(map.get())).onChange(text -> map.set(splitMap(text)));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <E extends Enum<E>> Widget enumControl(Setting<?> setting, EnumSettingType<?> type) {
		Setting<E> enumSetting = (Setting<E>) setting;
		EnumSettingType<E> enumType = (EnumSettingType<E>) type;
		return new CycleButton<>(null, enumType.values(), enumSetting.get(), value -> enumLabel(enumSetting, value), enumSetting::set);
	}

	public static Text enumLabel(Setting<?> setting, Enum<?> value) {
		return Text.translatable(setting.translationKey() + "." + value.name().toLowerCase(Locale.ROOT));
	}

	public static List<String> splitList(String text) {
		List<String> out = new ArrayList<>();
		for (String part : text.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				out.add(trimmed);
			}
		}
		return out;
	}

	public static String joinMap(Map<String, String> map) {
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			parts.add(entry.getKey() + "=" + entry.getValue());
		}
		return String.join(", ", parts);
	}

	public static Map<String, String> splitMap(String text) {
		Map<String, String> out = new LinkedHashMap<>();
		for (String part : splitList(text)) {
			int split = part.indexOf('=');
			if (split > 0) {
				out.put(part.substring(0, split).trim(), part.substring(split + 1).trim());
			}
		}
		return out;
	}

	public static boolean wide(Setting<?> setting) {
		SettingType<?> type = setting.type();
		return type instanceof StringSettingType || type instanceof StringListSettingType || type instanceof StringMapSettingType;
	}
}
