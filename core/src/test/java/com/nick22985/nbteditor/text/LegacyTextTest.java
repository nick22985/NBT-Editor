package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.util.LegacyTextUtil;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class LegacyTextTest {
	@Test
	void parsesCodes() {
		Text text = LegacyTextUtil.parse("§cRed §lBold§r plain");
		List<TextSpan> spans = text.flatten();
		assertEquals(3, spans.size());
		assertEquals(TextColor.of(Formatting.RED), spans.get(0).style().color());
		assertEquals(true, spans.get(1).style().isBold());
		assertEquals(Style.EMPTY, spans.get(2).style());
		assertEquals("Red Bold plain", text.plain());
	}

	@Test
	void colourResetsFlags() {
		Text text = LegacyTextUtil.parse("§lA§cB");
		assertEquals(false, text.flatten().get(1).style().isBold());
	}

	@Test
	void parsesHex() {
		Text text = LegacyTextUtil.parse("&#FF0000a&x&0&0&F&F&0&0b", '&', true);
		assertEquals(0xFF0000, text.flatten().get(0).style().color().rgb());
		assertEquals(0x00FF00, text.flatten().get(1).style().color().rgb());
	}

	@Test
	void roundTrip() {
		String legacy = "§6Gold §lbold§r§7 grey";
		assertEquals(LegacyTextUtil.parse(legacy).flatten(), LegacyTextUtil.parse(LegacyTextUtil.toLegacy(LegacyTextUtil.parse(legacy))).flatten());
	}
}
