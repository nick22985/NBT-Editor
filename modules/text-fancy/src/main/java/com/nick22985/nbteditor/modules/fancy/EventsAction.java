package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.ClickAction;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.HoverAction;
import com.nick22985.nbteditor.text.HoverEvent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.FormGrid;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.ClickEventUtil;
import java.util.List;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EventsAction implements RichTextAction {
	private static final String NONE = "none";
	private static final List<String> CLICKS = List.of(NONE, "open_url", "run_command", "suggest_command", "change_page", "copy_to_clipboard");
	private static final int HOVER_HEIGHT = 44;

	private final ModuleContext ctx;
	private final TextSyntax syntax;

	public EventsAction(ModuleContext ctx, TextSyntax syntax) {
		this.ctx = ctx;
		this.syntax = syntax;
	}

	@Override
	public Text label() {
		return this.ctx.tr("events");
	}

	@Override
	public Text tooltip() {
		return this.ctx.tr("events.tooltip");
	}

	@Override
	public void run(RichTextField field, Button button) {
		UiScreen screen = button.screen();
		if (screen == null) {
			return;
		}
		Style style = field.buffer().selectionStyle();
		ClickEvent click = style.clickEvent();
		HoverEvent hover = style.hoverEvent();
		ModalPanel panel = new ModalPanel(this.ctx.tr("events.title"), 300);
		FormGrid form = new FormGrid();
		String initial = click != null && CLICKS.contains(click.action().serializedName()) ? click.action().serializedName() : NONE;
		TextField value = new TextField(click == null || click.value() == null ? "" : click.value());
		Dropdown<String> action = form.row(this.ctx.tr("events.click"), new Dropdown<>(CLICKS, initial, id -> this.ctx.tr("events.click." + id), id -> value.validator(this.validator(id))));
		form.row(this.ctx.tr("events.value"), value);
		value.placeholder(this.ctx.tr("events.value.placeholder"));
		value.validator(this.validator(initial));
		RichTextField hoverText = form.row(this.ctx.tr("events.hover"), new RichTextField(true), HOVER_HEIGHT);
		hoverText.pasteParser(input -> this.syntax.recognizes(input) ? this.syntax.parse(input) : null);
		hoverText.copyFormatter(this.syntax::stringify);
		hoverText.placeholder(this.ctx.tr(hover != null && hover.action() != HoverAction.SHOW_TEXT ? "events.hover.kept" : "events.hover.placeholder"));
		hoverText.value(hover != null && hover.action() == HoverAction.SHOW_TEXT ? hover.text() : Text.EMPTY);
		TextField insertion = form.row(this.ctx.tr("events.insertion"), new TextField(style.insertion() == null ? "" : style.insertion()));
		insertion.placeholder(this.ctx.tr("events.insertion.placeholder"));
		panel.content(form, form.preferredHeight());
		panel.closingAction(Text.translatable("gui.cancel"), field::focus);
		panel.defaultAction(panel.action(Text.translatable("gui.done"), () -> {
			if (!value.valid()) {
				screen.setFocus(value);
				return;
			}
			panel.close();
			ClickEvent nextClick = NONE.equals(action.value()) || value.value().isEmpty() ? null : new ClickEvent(ClickAction.byName(action.value()), value.value());
			HoverEvent nextHover = hoverText.buffer().length() > 0 ? HoverEvent.showText(hoverText.value()) : hover != null && hover.action() != HoverAction.SHOW_TEXT ? hover : null;
			String nextInsertion = insertion.value().isEmpty() ? null : insertion.value();
			field.focus();
			field.style(current -> current.withClickEvent(nextClick).withHoverEvent(nextHover).withInsertion(nextInsertion));
		}));
		screen.openOverlay(panel);
		screen.setFocus(value);
	}

	private Function<String, Text> validator(String id) {
		ClickAction kind = ClickAction.byName(id);
		if (kind != ClickAction.OPEN_URL && kind != ClickAction.CHANGE_PAGE) {
			return null;
		}
		return input -> ClickEventUtil.validValue(kind, input) ? null : this.ctx.tr("events.value.invalid." + id);
	}
}
