package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Overlay;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;
import com.nick22985.nbteditor.util.WidgetSizeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ModalPanel extends Overlay {
	public static final int PAD_X = 8;
	public static final int PAD_Y = 7;
	public static final int SPACING = 5;
	private static final int ACTION_MIN_WIDTH = 56;
	private static final int ACTION_GAP = 3;

	private final Text title;
	private final int panelWidth;
	private final List<Button> actions = new ArrayList<>();
	private int titleColor = Theme.TEXT;
	private String[] icon;
	private Text message;
	private Text hint;
	private List<Text> messageLines = List.of();
	private Widget content;
	private int contentHeight;
	private int maxContentHeight;
	private int contentY;
	private Button defaultAction;
	private Runnable onCancel;
	private boolean dismissable = true;

	public ModalPanel(Text title, int width) {
		this.title = title;
		this.panelWidth = width;
	}

	public ModalPanel(Text title) {
		this(title, Theme.MODAL_WIDTH);
	}

	public ModalPanel warn(int color) {
		this.icon = PixelSprites.WARN;
		this.titleColor = color;
		return this;
	}

	public ModalPanel icon(String[] icon, int color) {
		this.icon = icon;
		this.titleColor = color;
		return this;
	}

	public ModalPanel message(Text message) {
		this.message = message;
		return this;
	}

	public ModalPanel hint(Text hint) {
		this.hint = hint;
		return this;
	}

	public ModalPanel keysHint() {
		return this.hint(Text.translatable("nbtedit.ui.modal.keys"));
	}

	public ModalPanel dismissable(boolean dismissable) {
		this.dismissable = dismissable;
		return this;
	}

	public ModalPanel onCancel(Runnable onCancel) {
		this.onCancel = onCancel;
		return this;
	}

	public <T extends Widget> T content(T content, int height) {
		return this.content(content, height, height);
	}

	public <T extends Widget> T content(T content, int height, int maxHeight) {
		if (this.content != null) {
			this.remove(this.content);
		}
		this.content = content;
		this.contentHeight = height;
		this.maxContentHeight = maxHeight;
		this.add(content);
		this.relayout();
		return content;
	}

	public void contentHeight(int height) {
		this.contentHeight = height;
		this.relayout();
	}

	public Button action(Text label, Runnable onPress) {
		Button button = new Button(label, onPress);
		this.actions.add(this.add(button));
		this.relayout();
		return button;
	}

	public Button closingAction(Text label, Runnable onPress) {
		return this.action(label, () -> {
			this.close();
			if (onPress != null) {
				onPress.run();
			}
		});
	}

	public Button dangerAction(Text label, Runnable onPress) {
		return this.action(TextStyleUtil.colored(label, Theme.RED), onPress);
	}

	public ModalPanel defaultAction(Button button) {
		this.defaultAction = button;
		return this;
	}

	public List<Button> actions() {
		return Collections.unmodifiableList(this.actions);
	}

	public int contentX() {
		return this.x + PAD_X;
	}

	public int contentY() {
		return this.contentY;
	}

	public int contentWidth() {
		return this.panelWidth - PAD_X * 2;
	}

	private void relayout() {
		if (this.screen() != null) {
			this.layout(this.screen().width(), this.screen().height());
		}
	}

	@Override
	public void layout(int screenWidth, int screenHeight) {
		UiHost host = this.host();
		int width = Math.min(this.panelWidth, screenWidth - 8);
		int innerWidth = width - PAD_X * 2;
		List<Text> lines = new ArrayList<>();
		if (this.message != null && host != null) {
			lines.addAll(host.wrap(this.message, innerWidth));
		}
		this.messageLines = lines;
		int bodyHeight = 9;
		if (!lines.isEmpty()) {
			bodyHeight += SPACING + lines.size() * Theme.LINE - 1;
		}
		int contentTop = bodyHeight;
		if (this.content != null && this.contentHeight > 0) {
			bodyHeight += SPACING + this.contentHeight;
			contentTop += SPACING;
		}
		if (!this.actions.isEmpty()) {
			bodyHeight += SPACING + 2 + Theme.BUTTON_HEIGHT;
		}
		int height = PAD_Y * 2 + bodyHeight;
		int x = (screenWidth - width) / 2;
		int y = Math.max(PAD_Y, (screenHeight - height - Math.max(0, this.maxContentHeight - this.contentHeight)) / 2);
		this.bounds(x, y, width, height);
		this.contentY = y + PAD_Y + contentTop;
		if (this.content != null) {
			this.content.bounds(x + PAD_X, this.contentY, innerWidth, this.contentHeight);
		}
		int actionX = x + width - PAD_X;
		int actionY = y + height - PAD_Y - Theme.BUTTON_HEIGHT;
		for (int i = this.actions.size() - 1; i >= 0; i--) {
			Button button = this.actions.get(i);
			int buttonWidth = host == null ? ACTION_MIN_WIDTH : Math.max(ACTION_MIN_WIDTH, WidgetSizeUtil.buttonWidth(host, button.label()) - 4);
			actionX -= buttonWidth;
			button.bounds(actionX, actionY, buttonWidth, Theme.BUTTON_HEIGHT);
			actionX -= ACTION_GAP;
		}
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.modalFrame(canvas, this.x, this.y, this.width, this.height);
		int textX = this.contentX();
		int textY = this.y + PAD_Y;
		if (this.icon != null) {
			DrawUtil.sprite(canvas, this.icon, textX, textY + 1, this.titleColor);
			textX += DrawUtil.spriteWidth(this.icon) + 4;
		}
		DrawUtil.textClipped(canvas, this.title, textX, textY, this.contentX() + this.contentWidth() - textX, this.titleColor);
		int lineY = textY + 9 + SPACING;
		for (Text line : this.messageLines) {
			DrawUtil.text(canvas, line, this.contentX(), lineY, Theme.MODAL_TEXT);
			lineY += Theme.LINE;
		}
		if (this.hint != null && !this.actions.isEmpty()) {
			int hintY = this.y + this.height - PAD_Y - Theme.BUTTON_HEIGHT + 4;
			int maxWidth = this.actions.get(0).x() - ACTION_GAP - this.contentX();
			DrawUtil.textClipped(canvas, this.hint, this.contentX(), hintY, maxWidth, Theme.DIM);
		}
	}

	@Override
	public boolean closeOnEscape() {
		return this.dismissable;
	}

	public void cancel() {
		this.close();
		if (this.onCancel != null) {
			this.onCancel.run();
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEnter() && this.defaultAction != null && this.defaultAction.interactive()) {
			this.defaultAction.press();
			return true;
		}
		if (input.is(Key.ESCAPE) && this.dismissable) {
			this.cancel();
			return true;
		}
		return false;
	}
}
