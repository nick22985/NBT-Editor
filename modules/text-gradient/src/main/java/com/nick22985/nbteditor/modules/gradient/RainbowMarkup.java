package com.nick22985.nbteditor.modules.gradient;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextMarkup;
import com.nick22985.nbteditor.util.GradientUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RainbowMarkup implements TextMarkup {
	@Override
	public Text apply(String value, Text contents) {
		float phase = 0;
		if (!value.isBlank()) {
			try {
				phase = Float.parseFloat(value.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (phase > 1 || phase < -1) {
			phase /= 360.0F;
		}
		phase = (phase % 1.0F + 1.0F) % 1.0F;
		int count = Math.max(2, contents.plain().length());
		return GradientUtil.apply(contents, GradientUtil.rainbow(count, phase));
	}
}
