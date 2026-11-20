package com.nick22985.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nick22985.nbteditor.text.ClickAction;
import org.junit.jupiter.api.Test;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class ClickEventUtilTest {
	@Test
	void urlsNeedHttpScheme() {
		assertTrue(ClickEventUtil.validValue(ClickAction.OPEN_URL, "https://example.com/a?b=c"));
		assertTrue(ClickEventUtil.validValue(ClickAction.OPEN_URL, "HTTP://example.com"));
		assertFalse(ClickEventUtil.validValue(ClickAction.OPEN_URL, "example.com"));
		assertFalse(ClickEventUtil.validValue(ClickAction.OPEN_URL, "file:///etc/passwd"));
		assertFalse(ClickEventUtil.validValue(ClickAction.OPEN_URL, "https://exa mple.com"));
	}

	@Test
	void pagesArePositive() {
		assertTrue(ClickEventUtil.validValue(ClickAction.CHANGE_PAGE, "3"));
		assertFalse(ClickEventUtil.validValue(ClickAction.CHANGE_PAGE, "0"));
		assertFalse(ClickEventUtil.validValue(ClickAction.CHANGE_PAGE, "two"));
	}

	@Test
	void otherActionsAndEmptyPass() {
		assertTrue(ClickEventUtil.validValue(ClickAction.RUN_COMMAND, "anything"));
		assertTrue(ClickEventUtil.validValue(ClickAction.OPEN_URL, ""));
	}
}
