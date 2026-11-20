package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TextWrapUtil;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelDetail extends WidgetGroup {
	private static final int LINE = 9;
	private static final int HINT_LINES = 2;

	private final PackModelsScreen screen;
	private final ModuleContext ctx;
	private final Button give;
	private final Button apply;
	private final Button copyId;
	private final Button copyGive;

	public ModelDetail(PackModelsScreen screen, ModuleContext ctx) {
		this.screen = screen;
		this.ctx = ctx;
		this.give = this.add(new Button(ctx.tr("models.give"), () -> this.act(screen::give)));
		this.apply = this.add(new Button(ctx.tr("models.apply"), () -> this.act(screen::apply)));
		this.copyId = this.add(new Button(ctx.tr("models.copy_id"), () -> this.act(screen::copyId)));
		this.copyGive = this.add(new Button(ctx.tr("models.copy_give"), () -> this.act(screen::copyGive)));
	}

	private void act(Consumer<ModelEntry> action) {
		ModelEntry entry = this.screen.selected();
		if (entry != null) {
			action.accept(entry);
		}
	}

	@Override
	protected void onLayout() {
		int half = (this.width - 3) / 2;
		int second = this.buttonsTop() + Theme.BUTTON_HEIGHT + 3;
		this.give.bounds(this.x, this.buttonsTop(), half, Theme.BUTTON_HEIGHT);
		this.apply.bounds(this.x + half + 3, this.buttonsTop(), this.width - half - 3, Theme.BUTTON_HEIGHT);
		this.copyId.bounds(this.x, second, half, Theme.BUTTON_HEIGHT);
		this.copyGive.bounds(this.x + half + 3, second, this.width - half - 3, Theme.BUTTON_HEIGHT);
	}

	private int buttonsTop() {
		return this.y + this.height - HINT_LINES * LINE - 2 - Theme.BUTTON_HEIGHT * 2 - 3;
	}

	public void refresh() {
		ModelEntry entry = this.screen.selected();
		boolean shown = entry != null;
		this.give.visible(shown);
		this.apply.visible(shown);
		this.copyId.visible(shown);
		this.copyGive.visible(shown);
		if (shown) {
			this.give.enabled(this.screen.creative());
			this.apply.enabled(this.screen.applyBlock(entry) == null);
		}
	}

	private boolean compact() {
		return this.buttonsTop() - this.y < 3 * 16 + 8 + 6 + 4 * (LINE + 1) + 3 + LINE + 5 + 3;
	}

	private int preview() {
		if (this.compact()) {
			return 2;
		}
		return this.width >= 170 ? 4 : 3;
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		ModelEntry entry = this.screen.selected();
		if (entry == null) {
			int top = this.y + 2;
			for (Text line : canvas.wrap(this.ctx.tr("models.select"), this.width)) {
				DrawUtil.text(canvas, line, this.x, top, Theme.FAINT);
				top += LINE + 1;
			}
			return;
		}
		int limit = this.buttonsTop() - 3;
		canvas.pushClip(this.x, this.y, this.width, limit - this.y);
		GameItem item = this.screen.item(entry);
		int scale = this.preview();
		int box = scale * 16 + 8;
		DrawUtil.well(canvas, this.x, this.y, box, box);
		canvas.pushClip(this.x + 1, this.y + 1, box - 2, box - 2);
		canvas.item(item, this.x + 4, this.y + 4, scale, false);
		canvas.popClip();
		int infoX = this.x + box + 6;
		int infoWidth = this.right() - infoX;
		int top = this.y + 1;
		List<Text> title = TextWrapUtil.wrap(canvas, Text.literal(entry.model().label()), infoWidth);
		for (int i = 0; i < Math.min(2, title.size()); i++) {
			DrawUtil.textClipped(canvas, title.get(i), infoX, top, infoWidth, Theme.TEXT);
			top += LINE + 1;
		}
		Text pack = entry.pack().isEmpty() ? this.ctx.tr("models.pack.unknown") : Text.literal(entry.pack());
		DrawUtil.textClipped(canvas, pack, infoX, top, infoWidth, Theme.FAINT);
		top += LINE + 1;
		String folder = this.screen.realFolder(entry);
		if (!folder.isEmpty()) {
			DrawUtil.textClipped(canvas, folder + "/", infoX, top, infoWidth, Theme.FAINT);
		}
		if (!this.compact()) {
			int slotY = this.y + box - Theme.SLOT;
			canvas.slot(infoX, slotY);
			canvas.pushClip(infoX + 1, slotY + 1, 16, 16);
			canvas.item(item, infoX + 1, slotY + 1, 1.0F, false);
			canvas.popClip();
			DrawUtil.textClipped(canvas, this.ctx.tr("models.in_slot"), infoX + Theme.SLOT + 4, slotY + 5, infoWidth - Theme.SLOT - 4, Theme.FAINT);
		}
		top = this.y + box + 6;
		top = this.pairs(canvas, entry, top);
		top = this.command(canvas, entry, top + 3, limit);
		if (entry.model().itemModel() != null) {
			top += 4;
			for (Text line : canvas.wrap(this.ctx.tr("models.note.item_model"), this.width - 5)) {
				DrawUtil.rect(canvas, this.x, top - 1, 1, LINE, Theme.GOLD);
				DrawUtil.text(canvas, line, this.x + 4, top, Theme.GOLD);
				top += LINE;
			}
		}
		canvas.popClip();
		Text hint = this.screen.hint(entry);
		if (hint != null) {
			int hintY = this.bottom() - HINT_LINES * LINE;
			List<Text> lines = canvas.wrap(hint, this.width);
			for (int i = 0; i < Math.min(HINT_LINES, lines.size()); i++) {
				DrawUtil.textClipped(canvas, lines.get(i), this.x, hintY + i * LINE, this.width, Theme.FAINT);
			}
		}
	}

	private int pairs(Canvas canvas, ModelEntry entry, int top) {
		Text[] keys = {this.ctx.tr("models.base_item"), this.ctx.tr("models.item_model"), this.ctx.tr("models.model_data"), this.ctx.tr("models.model")};
		String[] values = {entry.model().item(), entry.model().itemModel(), entry.model().value() == null ? null : String.valueOf(entry.model().value()), entry.model().model().isEmpty() ? null : entry.model().model()};
		int keyWidth = 0;
		for (Text key : keys) {
			keyWidth = Math.max(keyWidth, canvas.textWidth(key));
		}
		keyWidth = Math.min(keyWidth + 6, this.width / 2);
		for (int i = 0; i < keys.length; i++) {
			DrawUtil.textClipped(canvas, keys[i], this.x, top, keyWidth - 4, Theme.LABEL);
			if (values[i] == null) {
				DrawUtil.textClipped(canvas, this.ctx.tr("models.value.none"), this.x + keyWidth, top, this.width - keyWidth, Theme.DIM);
			} else {
				DrawUtil.textClipped(canvas, values[i], this.x + keyWidth, top, this.width - keyWidth, Theme.TEXT);
			}
			top += LINE + 1;
		}
		return top;
	}

	private int command(Canvas canvas, ModelEntry entry, int top, int limit) {
		List<Text> lines = TextWrapUtil.wrap(canvas, Text.literal("/" + this.screen.giveCommand(entry)), this.width - 8);
		int room = (limit - top - 5) / LINE;
		if (room < 1) {
			return top;
		}
		int count = Math.min(lines.size(), Math.min(room, 4));
		int height = count * LINE + 5;
		DrawUtil.field(canvas, this.x, top, this.width, height, Theme.FIELD_BORDER_READ_ONLY);
		for (int i = 0; i < count; i++) {
			DrawUtil.text(canvas, lines.get(i), this.x + 4, top + 3 + i * LINE, Theme.FIELD_TEXT);
		}
		return top + height;
	}
}
