package com.nick22985.nbteditor.fabric.ui;

import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.util.TextWrapUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class McTextMetrics implements TextMetrics {
	protected Font font() {
		return Minecraft.getInstance().font;
	}

	@Override
	public int textWidth(String text) {
		return this.font().width(text);
	}

	@Override
	public int textWidth(Text text) {
		return this.font().width(McTextUtil.sequence(text));
	}

	@Override
	public List<Text> wrap(Text text, int width) {
		return TextWrapUtil.wrap(this, McTextUtil.resolved(text), width);
	}

	@Override
	public String trimToWidth(String text, int width) {
		return TextWrapUtil.trim(this, text, width);
	}

	@Override
	public Text trimToWidth(Text text, int width) {
		return TextWrapUtil.trim(this, McTextUtil.resolved(text), width);
	}
}
