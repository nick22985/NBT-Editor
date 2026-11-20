package com.nick22985.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.ui.Theme;
import org.junit.jupiter.api.Test;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class SnbtHighlightUtilTest {
	@Test
	void legacyOutOfRangeNumbersAreStrings() {
		assertEquals(TextStyleUtil.colored("300b", Theme.GREEN), SnbtHighlightUtil.highlight("300b", SnbtDialect.LEGACY));
		assertEquals(TextStyleUtil.colored("99999999999", Theme.GREEN), SnbtHighlightUtil.highlight("99999999999", SnbtDialect.LEGACY));
		assertEquals(SnbtHighlightUtil.number("100b"), SnbtHighlightUtil.highlight("100b", SnbtDialect.LEGACY));
		assertEquals(SnbtHighlightUtil.number("5"), SnbtHighlightUtil.highlight("5", SnbtDialect.LEGACY));
	}

	@Test
	void modernKeepsNumberColors() {
		assertNotEquals(TextStyleUtil.colored("300b", Theme.GREEN), SnbtHighlightUtil.highlight("300b", SnbtDialect.MODERN));
	}
}
