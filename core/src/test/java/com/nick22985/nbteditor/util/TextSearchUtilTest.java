package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.ui.text.TextMatch;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class TextSearchUtilTest {
	@Test
	void findsNextAndWraps() {
		Pattern pattern = TextSearchUtil.pattern("a.b", false);
		String text = "a.b xx a.b axb";
		assertEquals(new TextMatch(0, 3), TextSearchUtil.next(pattern, text, 0));
		assertEquals(new TextMatch(7, 10), TextSearchUtil.next(pattern, text, 3));
		assertEquals(new TextMatch(0, 3), TextSearchUtil.next(pattern, text, 10));
		assertNull(TextSearchUtil.next(TextSearchUtil.pattern("zz", false), text, 0));
	}

	@Test
	void regexSkipsEmptyMatchesAndUsesLines() {
		Pattern pattern = TextSearchUtil.pattern("^\\s*", true);
		assertEquals(new TextMatch(4, 6), TextSearchUtil.next(pattern, "foo\n  bar", 0));
		assertNull(TextSearchUtil.next(TextSearchUtil.pattern("x*", true), "abc", 0));
	}

	@Test
	void expandsGroupsForOneMatch() {
		Pattern pattern = TextSearchUtil.pattern("(\\w+):(\\d+)", true);
		String text = "id:1 lvl:5";
		assertEquals("5=lvl", TextSearchUtil.expand(pattern, text, new TextMatch(5, 10), "$2=$1", true));
		assertNull(TextSearchUtil.expand(pattern, text, new TextMatch(5, 9), "$2", true));
		assertEquals("$1", TextSearchUtil.expand(TextSearchUtil.pattern("lvl", false), text, new TextMatch(5, 8), "$1", false));
	}

	@Test
	void replacesAll() {
		assertEquals("b.b b.b", TextSearchUtil.replaceAll(TextSearchUtil.pattern("a", false), "a.b a.b", "b", false));
		assertEquals("Lvl:5", TextSearchUtil.replaceAll(TextSearchUtil.pattern("l(v)l", true), "lvl:5", "L$1l", true));
	}
}
