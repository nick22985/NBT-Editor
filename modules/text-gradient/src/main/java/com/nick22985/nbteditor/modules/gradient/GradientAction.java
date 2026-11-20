package com.nick22985.nbteditor.modules.gradient;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.text.RichBuffer;
import com.nick22985.nbteditor.ui.text.RichCell;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.FormGrid;
import com.nick22985.nbteditor.ui.widget.Label;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.util.GradientUtil;
import com.nick22985.nbteditor.util.RichTextUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GradientAction implements RichTextAction {
	private static final String CUSTOM = "custom";
	private static final List<String> PRESETS = List.of(CUSTOM, "rainbow", "fire", "ocean", "sunset", "forest", "candy", "mono");

	private final ModuleContext ctx;
	private int[] last = {0xFF5555, 0x5555FF};
	private boolean perLine = true;

	public GradientAction(ModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public Text label() {
		return this.ctx.tr("action");
	}

	@Override
	public Text tooltip() {
		return this.ctx.tr("action.tooltip");
	}

	private int[] preset(String id) {
		switch (id) {
			case "rainbow":
				return GradientUtil.rainbow(7, 0);
			case "fire":
				return new int[] {0xFFE259, 0xFF6A00, 0xC0392B};
			case "ocean":
				return new int[] {0x00C9FF, 0x0072FF, 0x1A2980};
			case "sunset":
				return new int[] {0xF83600, 0xF9D423};
			case "forest":
				return new int[] {0x96C93D, 0x00B09B, 0x0B6623};
			case "candy":
				return new int[] {0xFBC2EB, 0xA6C1EE};
			case "mono":
				return new int[] {0xFFFFFF, 0x555555};
			default:
				return this.last;
		}
	}

	@Override
	public void run(RichTextField field, Button button) {
		UiScreen screen = button.screen();
		if (screen == null) {
			return;
		}
		RichBuffer buffer = field.buffer();
		Text sample = buffer.hasSelection() ? buffer.selectedText() : buffer.length() > 0 ? buffer.toText() : this.ctx.tr("sample");
		Text preview = RichTextUtil.lines(RichTextUtil.cells(sample)).get(0);
		ModalPanel panel = new ModalPanel(this.ctx.tr("title"), 280);
		FormGrid form = new FormGrid();
		GradientStops stops = form.row(this.ctx.tr("colors"), new GradientStops(this.ctx, this.last), 20);
		Dropdown<String> preset = form.row(this.ctx.tr("preset"), new Dropdown<>(PRESETS, CUSTOM, id -> this.ctx.tr("preset." + id), id -> {
			if (!CUSTOM.equals(id)) {
				stops.stops(this.preset(id));
			}
		}));
		preset.columns(2);
		stops.onChange(() -> preset.value(CUSTOM));
		form.row(this.ctx.tr("preview"), new Label(() -> GradientUtil.apply(preview, stops.stops())));
		Checkbox lines = form.row(null, new Checkbox(this.ctx.tr("per_line"), this.perLine, value -> {
		}));
		lines.visible(buffer.multiline());
		panel.content(form, form.preferredHeight());
		panel.closingAction(Text.translatable("gui.cancel"), field::focus);
		panel.defaultAction(panel.closingAction(Text.translatable("gui.done"), () -> {
			this.last = stops.stops();
			this.perLine = lines.checked();
			field.focus();
			this.apply(field, this.last, buffer.multiline() && this.perLine);
		}));
		screen.openOverlay(panel);
	}

	private void apply(RichTextField field, int[] stops, boolean perLine) {
		if (!field.editable()) {
			return;
		}
		RichBuffer buffer = field.buffer();
		int start = buffer.hasSelection() ? buffer.selectionStart() : 0;
		int end = buffer.hasSelection() ? buffer.selectionEnd() : buffer.length();
		if (start >= end) {
			return;
		}
		List<RichCell> cells = buffer.cells().subList(start, end);
		List<Style> styles = new ArrayList<>(cells.size());
		if (perLine) {
			int lineStart = 0;
			for (int i = 0; i <= cells.size(); i++) {
				if (i == cells.size() || cells.get(i).isNewline()) {
					styles.addAll(GradientUtil.styles(cells.subList(lineStart, i), stops));
					if (i < cells.size()) {
						styles.add(cells.get(i).style());
					}
					lineStart = i + 1;
				}
			}
		} else {
			styles.addAll(GradientUtil.styles(cells, stops));
		}
		buffer.restyle(start, end, styles);
	}
}
