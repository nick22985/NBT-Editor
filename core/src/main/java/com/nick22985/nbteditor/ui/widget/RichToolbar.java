package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class RichToolbar extends WidgetGroup {
	private static final Formatting[] TOGGLES = {Formatting.BOLD, Formatting.ITALIC, Formatting.UNDERLINE, Formatting.STRIKETHROUGH, Formatting.OBFUSCATED};
	private static final int TOGGLE_WIDTH = 16;
	private static final int COLOR_WIDTH = 24;
	private static final int SPACING = 2;

	private RichTextField field;
	private final Button color;
	private final List<Button> toggles = new ArrayList<>();
	private final List<RichTextAction> actionSources = new ArrayList<>();
	private final List<Button> actions = new ArrayList<>();
	private final Button more;
	private boolean measured;

	public RichToolbar(RichTextField field, List<RichTextAction> actions) {
		this.field = field;
		this.height = Theme.BUTTON_HEIGHT;
		this.color = this.add(new Button(Text.EMPTY, this::openColors));
		this.color.tooltip(Text.translatable("nbtedit.ui.rich.color"));
		for (Formatting formatting : TOGGLES) {
			Button toggle = this.add(new Button(this.toggleLabel(formatting), () -> this.toggle(formatting)));
			toggle.tooltip(Text.translatable("nbtedit.ui.rich." + formatting.serializedName()));
			this.toggles.add(toggle);
		}
		for (RichTextAction action : actions) {
			if (!action.applies(field)) {
				continue;
			}
			Button button = this.add(new Button(action.label(), pressed -> this.runAction(action, pressed)));
			if (action.tooltip() != null) {
				button.tooltip(action.tooltip());
			}
			this.actionSources.add(action);
			this.actions.add(button);
		}
		this.more = this.add(new Button(Text.literal("…"), this::openMore));
		this.more.tooltip(Text.translatable("nbtedit.ui.rich.more"));
		this.more.visible(false);
	}

	public RichTextField target() {
		return this.field;
	}

	public RichToolbar target(RichTextField field) {
		this.field = field;
		return this;
	}

	private Text toggleLabel(Formatting formatting) {
		switch (formatting) {
			case BOLD:
				return Text.literal("B", Style.EMPTY.withBold(true));
			case ITALIC:
				return Text.literal("I", Style.EMPTY.withItalic(true));
			case UNDERLINE:
				return Text.literal("U", Style.EMPTY.withUnderlined(true));
			case STRIKETHROUGH:
				return Text.literal("S", Style.EMPTY.withStrikethrough(true));
			default:
				return Text.literal("?");
		}
	}

	private void toggle(Formatting formatting) {
		this.field.toggle(formatting);
		this.field.focus();
	}

	private void runAction(RichTextAction action, Button button) {
		action.run(this.field, button);
	}

	private void openMore(Button anchor) {
		List<MenuOption> options = new ArrayList<>();
		for (int i = 0; i < this.actions.size(); i++) {
			Button button = this.actions.get(i);
			if (button.visible()) {
				continue;
			}
			RichTextAction action = this.actionSources.get(i);
			MenuOption option = MenuOption.of(action.label(), () -> this.runAction(action, anchor));
			options.add(action.tooltip() == null ? option : option.tooltip(action.tooltip()));
		}
		ModalUtil.popover(anchor, options);
	}

	private void openColors() {
		UiScreen screen = this.screen();
		if (screen == null) {
			return;
		}
		List<MenuOption> options = new ArrayList<>();
		TextColor current = this.field.buffer().selectionStyle().color();
		for (Formatting formatting : Formatting.values()) {
			if (!formatting.isColor()) {
				continue;
			}
			Text label = Text.literal("█ ", Style.EMPTY.withColor(formatting)).append(Text.translatable("nbtedit.ui.color." + formatting.serializedName()));
			options.add(MenuOption.of(label, () -> this.pick(TextColor.of(formatting))).checked(current != null && current.equals(TextColor.of(formatting))));
		}
		options.add(MenuOption.of(Text.translatable("nbtedit.ui.color.custom"), this::openCustom).checked(current != null && current.named() == null));
		options.add(MenuOption.of(Text.translatable("nbtedit.ui.color.reset"), () -> this.pick(null)).checked(current == null));
		screen.openOverlay(new PopoverMenu(this.color.x(), this.color.bottom(), false, options).columns(2));
	}

	private void openCustom() {
		UiScreen screen = this.screen();
		if (screen == null) {
			return;
		}
		Style style = this.field.currentStyle();
		int initial = style.color() == null ? 0xFFFFFF : style.color().rgb();
		ModalUtil.color(screen, Text.translatable("nbtedit.ui.color.title"), initial, rgb -> this.pick(TextColor.rgb(rgb)));
	}

	private void pick(TextColor color) {
		this.field.color(color);
		this.field.focus();
	}

	public int preferredWidth(TextMetrics metrics) {
		int width = COLOR_WIDTH + (TOGGLE_WIDTH + SPACING) * TOGGLES.length;
		for (Button button : this.actions) {
			width += SPACING + this.actionWidth(metrics, button);
		}
		return width;
	}

	private int actionWidth(TextMetrics metrics, Button button) {
		return Math.max(TOGGLE_WIDTH, metrics.textWidth(button.label()) + 10);
	}

	@Override
	protected void onLayout() {
		this.measured = false;
		int left = this.x;
		this.color.bounds(left, this.y, COLOR_WIDTH, this.height);
		left += COLOR_WIDTH + SPACING;
		for (Button toggle : this.toggles) {
			toggle.bounds(left, this.y, TOGGLE_WIDTH, this.height);
			left += TOGGLE_WIDTH + SPACING;
		}
	}

	private void measure(TextMetrics metrics) {
		if (this.measured) {
			return;
		}
		this.measured = true;
		int start = this.x + COLOR_WIDTH + (TOGGLE_WIDTH + SPACING) * (TOGGLES.length + 1) - SPACING;
		int right = this.x + this.width;
		int total = start - SPACING;
		for (Button button : this.actions) {
			total += SPACING + this.actionWidth(metrics, button);
		}
		boolean overflow = total > right;
		int limit = overflow ? right - TOGGLE_WIDTH - SPACING : right;
		int left = start;
		boolean fits = true;
		for (Button button : this.actions) {
			int width = this.actionWidth(metrics, button);
			fits = fits && left + width <= limit;
			button.visible(fits);
			button.bounds(left, this.y, width, this.height);
			if (fits) {
				left += width + SPACING;
			}
		}
		this.more.visible(overflow);
		this.more.bounds(left, this.y, TOGGLE_WIDTH, this.height);
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.measure(canvas);
		boolean editable = this.field.editable();
		this.color.enabled(editable);
		for (int i = 0; i < TOGGLES.length; i++) {
			Button toggle = this.toggles.get(i);
			toggle.enabled(editable);
			toggle.selected(this.field.active(TOGGLES[i]));
		}
		for (Button button : this.actions) {
			button.enabled(editable);
		}
		this.more.enabled(editable);
	}

	@Override
	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
		Style style = this.field.currentStyle();
		int rgb = style.color() == null ? 0xFFFFFF : style.color().rgb();
		DrawUtil.swatch(canvas, this.color.x() + 6, this.color.y() + 4, this.color.width() - 12, this.color.height() - 8, rgb, false);
	}
}
