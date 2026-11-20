package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.module.Registry;
import com.nick22985.nbteditor.text.ClickAction;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.HoverEvent;
import com.nick22985.nbteditor.text.KeybindContent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.text.TextMarkup;
import com.nick22985.nbteditor.ui.text.RichCell;
import com.nick22985.nbteditor.util.RichTextUtil;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class FancyTextSyntaxTest {
	private final FancyTextSyntax syntax = new FancyTextSyntax(new FancyGrammar(this.markups()));

	private Registry<TextMarkup> markups() {
		Registry<TextMarkup> registry = new Registry<>(TextMarkup.REGISTRY);
		registry.register("test", "red", (value, contents) -> Text.literal(contents.plain(), Style.EMPTY.withColor(Formatting.RED)), 0);
		return registry;
	}

	private List<RichCell> cells(String input) {
		return RichTextUtil.cells(this.syntax.parse(input));
	}

	private void roundTrip(String input) {
		Text parsed = this.syntax.parse(input);
		String written = this.syntax.stringify(parsed);
		assertTrue(RichTextUtil.same(parsed, this.syntax.parse(written)), input + " -> " + written);
	}

	@Test
	void legacyCodes() {
		List<RichCell> cells = this.cells("&lA&cB&#123456C");
		assertTrue(cells.get(0).style().isBold());
		assertEquals(TextColor.of(Formatting.RED), cells.get(1).style().color());
		assertEquals(false, cells.get(1).style().isBold());
		assertEquals(0x123456, cells.get(2).style().color().rgb());
	}

	@Test
	void spigotHex() {
		assertEquals(0xABCDEF, this.cells("&x&a&b&c&d&e&fX").get(0).style().color().rgb());
	}

	@Test
	void escapes() {
		assertEquals("&c[x]", this.syntax.parse("\\&c\\[x]").plain());
		assertEquals("a & b", this.syntax.parse("a & b").plain());
	}

	@Test
	void clickGroupScopesFormatting() {
		List<RichCell> cells = this.cells("[open_url]{https://a.b}(&cX)Y");
		assertEquals(new ClickEvent(ClickAction.OPEN_URL, "https://a.b"), cells.get(0).style().clickEvent());
		assertEquals(TextColor.of(Formatting.RED), cells.get(0).style().color());
		assertNull(cells.get(1).style().clickEvent());
		assertNull(cells.get(1).style().color());
	}

	@Test
	void unknownGroupIsLiteral() {
		assertEquals("[nope]{x}(y)", this.syntax.parse("[nope]{x}(y)").plain());
		assertEquals("[open_url]{x}(unclosed", this.syntax.parse("[open_url]{x}(unclosed").plain());
	}

	@Test
	void hoverText() {
		HoverEvent hover = this.cells("[show_text]{&aHi \\}}(X)").get(0).style().hoverEvent();
		assertEquals("Hi }", hover.text().plain());
	}

	@Test
	void atoms() {
		RichCell cell = this.cells("[keybind]{key.jump}").get(0);
		assertEquals(new KeybindContent("key.jump"), cell.atom());
	}

	@Test
	void miniMessage() {
		List<RichCell> cells = this.cells("<red>A<b>B</b>C</red>D");
		assertEquals(TextColor.of(Formatting.RED), cells.get(0).style().color());
		assertTrue(cells.get(1).style().isBold());
		assertEquals(false, cells.get(2).style().isBold());
		assertEquals(TextColor.of(Formatting.RED), cells.get(2).style().color());
		assertNull(cells.get(3).style().color());
		assertEquals("<nope>", this.syntax.parse("<nope>").plain());
	}

	@Test
	void markupGroups() {
		assertEquals(TextColor.of(Formatting.RED), this.cells("[red]{}(X)").get(0).style().color());
		assertEquals(TextColor.of(Formatting.RED), this.cells("<red>X").get(0).style().color());
	}

	@Test
	void roundTrips() {
		this.roundTrip("&lBold &cred&r plain & more");
		this.roundTrip("&#FF8800orange;semi");
		this.roundTrip("[run_command]{/say \\}hi}(&eClick \\) me)[show_text]{&b[x]}(hover)");
		this.roundTrip("[translate]{item.minecraft.diamond|&aone}[score]{@s|kills}[nbt]{entity|@s|Pos|interpret}");
		this.roundTrip("\\[open_url]{x}(y) \\<red> back\\\\slash");
		this.roundTrip("&_#80FF0000;shadow&r none");
		this.roundTrip("[font]{minecraft:alt}([insertion]{hi}(&lX))");
	}
}
