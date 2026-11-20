package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.text.LiteralContent;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.util.TextCodecUtil;
//? if >=1.20.5 {
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.ComponentSerialization;
//?}
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}
import net.minecraft.util.FormattedCharSequence;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McTextUtil {
	private static final int CACHE_LIMIT = 1024;
	private static final Map<Text, Component> COMPONENTS = new ConcurrentHashMap<>();
	private static final Map<Text, FormattedCharSequence> SEQUENCES = new ConcurrentHashMap<>();
	private static final Map<Text, Text> RESOLVED = new ConcurrentHashMap<>();
	private static volatile TextDialect dialect;
	private static volatile Language language;

	private McTextUtil() {
	}

	public static TextDialect dialect() {
		TextDialect current = dialect;
		if (current == null) {
			current = TextDialect.of(McVersionUtil.current());
			dialect = current;
		}
		return current;
	}

	public static MutableComponent literal(String text) {
		//? if >=1.19 {
		return Component.literal(text);
		//?} else
		//return new TextComponent(text);
	}

	public static Component translatable(String key, Object... args) {
		//? if >=1.19 {
		return Component.translatable(key, args);
		//?} else
		//return new TranslatableComponent(key, args);
	}

	public static Component toMc(Text text) {
		if (text == null) {
			return literal("");
		}
		if (text.siblings().isEmpty() && text.style().isEmpty() && text.content() instanceof LiteralContent literal) {
			return literal(literal.text());
		}
		McTextUtil.checkLanguage();
		Component cached = COMPONENTS.get(text);
		if (cached != null) {
			return cached;
		}
		Component built = fromJsonComponent(TextCodecUtil.toJson(text, dialect()));
		if (built == null) {
			built = literal(text.plain());
		}
		if (COMPONENTS.size() >= CACHE_LIMIT) {
			COMPONENTS.clear();
		}
		COMPONENTS.put(text, built);
		return built;
	}

	public static FormattedCharSequence sequence(Text text) {
		McTextUtil.checkLanguage();
		FormattedCharSequence cached = SEQUENCES.get(text);
		if (cached != null) {
			return cached;
		}
		FormattedCharSequence built = toMc(text).getVisualOrderText();
		if (SEQUENCES.size() >= CACHE_LIMIT) {
			SEQUENCES.clear();
		}
		SEQUENCES.put(text, built);
		return built;
	}

	public static Text resolved(Text text) {
		if (text == null || literalOnly(text)) {
			return text;
		}
		McTextUtil.checkLanguage();
		Text cached = RESOLVED.get(text);
		if (cached != null) {
			return cached;
		}
		List<Text> parts = new ArrayList<>();
		toMc(text).visit((style, part) -> {
			if (!part.isEmpty()) {
				parts.add(fromMc(literal(part).setStyle(style)));
			}
			return Optional.empty();
		}, Style.EMPTY);
		Text built = Text.join(parts);
		if (RESOLVED.size() >= CACHE_LIMIT) {
			RESOLVED.clear();
		}
		RESOLVED.put(text, built);
		return built;
	}

	private static void checkLanguage() {
		Language current = Language.getInstance();
		if (current != language) {
			language = current;
			COMPONENTS.clear();
			SEQUENCES.clear();
			RESOLVED.clear();
		}
	}

	private static boolean literalOnly(Text text) {
		if (!(text.content() instanceof LiteralContent)) {
			return false;
		}
		for (Text sibling : text.siblings()) {
			if (!literalOnly(sibling)) {
				return false;
			}
		}
		return true;
	}

	public static Text fromMc(Component component) {
		if (component == null) {
			return Text.EMPTY;
		}
		String json = toJson(component);
		return json == null ? Text.literal(component.getString()) : TextCodecUtil.fromJson(json);
	}

	public static String toJson(Component component) {
		try {
			//? if >=1.20.5 {
			return componentCodecOps().map(ops -> ComponentSerialization.CODEC.encodeStart(ops, component).result().map(JsonElement::toString).orElse(null)).orElse(null);
			//?} else
			//return Component.Serializer.toJson(component);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static Component fromJsonComponent(String json) {
		try {
			//? if >=1.20.5 {
			JsonElement tree = JsonParser.parseString(json);
			return componentCodecOps().flatMap(ops -> ComponentSerialization.CODEC.parse(ops, tree).result()).orElse(null);
			//?} else
			//return Component.Serializer.fromJson(json);
		} catch (RuntimeException e) {
			return null;
		}
	}

	//? if >=1.20.5 {
	private static Optional<DynamicOps<JsonElement>> componentCodecOps() {
		return Optional.of(NbteRuntime.INSTANCE.registries().get().createSerializationContext(JsonOps.INSTANCE));
	}
	//?}
}
