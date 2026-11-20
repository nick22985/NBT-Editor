package com.nick22985.nbteditor.modules.misc;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.util.TextCodecUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Signature {
	public static final Text DEFAULT = Text.literal("Made with NBT Editor", Style.EMPTY.withItalic(false).withColor(Formatting.DARK_GRAY));

	private final Setting<String> stored;

	public Signature(Setting<String> stored) {
		this.stored = stored;
	}

	public Text get() {
		String json = this.stored.get();
		if (json == null || json.isEmpty()) {
			return DEFAULT;
		}
		Text text = TextCodecUtil.fromJson(json);
		return text == null ? DEFAULT : text;
	}

	public void set(Text text) {
		this.stored.set(text == null || text.plain().isEmpty() ? "" : TextCodecUtil.toJson(text, TextDialect.LATEST));
	}
}
