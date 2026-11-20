package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.WidgetSizeUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class PanelScreen extends UiScreen {
	protected int bodyX;
	protected int bodyY;
	protected int bodyWidth;
	protected int bodyHeight;
	protected int footerY;
	private int footerRight;

	protected PanelScreen(Text title) {
		super(title);
	}

	@Override
	protected final void build() {
		this.bodyX = Theme.PAD;
		this.bodyY = Theme.PAD + Theme.FIELD_HEIGHT + Theme.GAP + 2 + Theme.GAP;
		this.bodyWidth = this.width - Theme.PAD * 2;
		this.footerY = this.height - Theme.PAD_BOTTOM - Theme.BUTTON_HEIGHT;
		this.bodyHeight = this.footerY - Theme.GAP - 2 - Theme.GAP - this.bodyY;
		this.footerRight = this.width - Theme.PAD;
		this.buildPanel();
	}

	protected abstract void buildPanel();

	protected Text subtitle() {
		return null;
	}

	protected int headerRight() {
		return this.width - Theme.PAD;
	}

	protected Button footerButton(Text label, Runnable onPress) {
		Button button = new Button(label, onPress);
		int buttonWidth = WidgetSizeUtil.buttonWidth(this.host(), label);
		this.footerRight -= buttonWidth;
		this.add(button).bounds(this.footerRight, this.footerY, buttonWidth, Theme.BUTTON_HEIGHT);
		this.footerRight -= 3;
		return button;
	}

	protected Button doneButton() {
		return this.footerButton(Text.translatable("gui.done"), this::close);
	}

	protected int footerLeftWidth() {
		return this.footerRight - Theme.GAP - Theme.PAD;
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.text(canvas, this.title(), Theme.PAD + 1, Theme.PAD + 4, Theme.TEXT);
		Text subtitle = this.subtitle();
		if (subtitle != null) {
			int subtitleX = Theme.PAD + 1 + canvas.textWidth(this.title()) + 8;
			DrawUtil.textClipped(canvas, subtitle, subtitleX, Theme.PAD + 4, this.headerRight() - subtitleX, Theme.FAINT);
		}
		DrawUtil.separator(canvas, 0, Theme.PAD + Theme.FIELD_HEIGHT + Theme.GAP, this.width);
		DrawUtil.separator(canvas, 0, this.footerY - Theme.GAP - 2, this.width);
		this.renderPanelBackground(canvas, mouseX, mouseY, delta);
	}

	protected void renderPanelBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
	}
}
