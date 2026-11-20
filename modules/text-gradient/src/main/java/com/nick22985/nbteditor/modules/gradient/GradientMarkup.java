package com.nick22985.nbteditor.modules.gradient;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextMarkup;
import com.nick22985.nbteditor.util.GradientUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GradientMarkup implements TextMarkup {
	@Override
	public Text apply(String value, Text contents) {
		int[] stops = GradientUtil.parseStops(value);
		return stops == null ? null : GradientUtil.apply(contents, stops);
	}
}
