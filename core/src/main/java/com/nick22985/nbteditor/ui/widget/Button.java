package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Button extends Widget {
	private Text label;
	private Consumer<Button> onPress;
	private ButtonStyle style = ButtonStyle.VANILLA;
	private String[] icon;
	private int iconColor = Theme.TEXT;
	private Integer labelColor;
	private boolean selected;
	private boolean leftAligned;
	private boolean clipped;

	public Button(Text label, Consumer<Button> onPress) {
		this.label = label;
		this.onPress = onPress;
		this.height = Theme.BUTTON_HEIGHT;
	}

	public Button(Text label, Runnable onPress) {
		this(label, button -> onPress.run());
	}

	public Text label() {
		return this.label;
	}

	public Button label(Text label) {
		this.label = label;
		return this;
	}

	public Button onPress(Consumer<Button> onPress) {
		this.onPress = onPress;
		return this;
	}

	public Button style(ButtonStyle style) {
		this.style = style;
		return this;
	}

	public ButtonStyle style() {
		return this.style;
	}

	public Button icon(String[] icon, int color) {
		this.icon = icon;
		this.iconColor = color;
		return this;
	}

	public Button labelColor(int color) {
		this.labelColor = color;
		return this;
	}

	public Button leftAligned(boolean leftAligned) {
		this.leftAligned = leftAligned;
		return this;
	}

	public Button selected(boolean selected) {
		this.selected = selected;
		return this;
	}

	public boolean selected() {
		return this.selected;
	}

	@Override
	public boolean focusable() {
		return this.style == ButtonStyle.VANILLA;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		boolean hovered = this.contains(mouseX, mouseY) && this.enabled();
		boolean active = this.interactive();
		int color;
		switch (this.style) {
			case FLAT:
				if (hovered || this.selected) {
					DrawUtil.rect(canvas, this.x, this.y, this.width, this.height, this.selected ? Theme.ROW_SELECTED : Theme.ROW_HOVER);
				}
				color = !active ? Theme.DIM : hovered || this.selected ? Theme.TEXT : Theme.LABEL;
				break;
			case LINK:
				color = !active ? Theme.DIM : hovered ? Theme.YELLOW : Theme.AQUA;
				break;
			default:
				canvas.button(this.x, this.y, this.width, this.height, hovered || this.focused(), active);
				color = active ? Theme.TEXT : Theme.LABEL;
				break;
		}
		if (this.labelColor != null && active) {
			color = this.labelColor;
		}
		if (this.selected && this.style == ButtonStyle.VANILLA && active) {
			color = Theme.YELLOW;
		}
		int contentWidth = 0;
		int iconWidth = this.icon == null ? 0 : DrawUtil.spriteWidth(this.icon);
		boolean hasLabel = this.label != null && !this.label.isEmpty();
		int labelWidth = hasLabel ? canvas.textWidth(this.label) : 0;
		contentWidth = iconWidth + (hasLabel && iconWidth > 0 ? 3 : 0) + labelWidth;
		int maxWidth = this.width - 6;
		int startX = this.leftAligned ? this.x + 4 : this.x + Math.max(3, (this.width - Math.min(contentWidth, maxWidth)) / 2);
		if (this.icon != null) {
			int iconY = this.y + (this.height - DrawUtil.spriteHeight(this.icon)) / 2;
			DrawUtil.sprite(canvas, this.icon, startX, iconY, active ? this.iconColor : Theme.DIM);
			startX += iconWidth + 3;
		}
		this.clipped = hasLabel && labelWidth > this.x + this.width - 3 - startX;
		if (hasLabel) {
			DrawUtil.textClipped(canvas, this.label, startX, this.y + (this.height - 8) / 2, this.x + this.width - 3 - startX, color);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		List<Text> tooltip = super.tooltip(mouseX, mouseY);
		return tooltip == null && this.clipped ? List.of(this.label) : tooltip;
	}

	public void press() {
		if (this.onPress != null) {
			this.onPress.accept(this);
		}
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		this.playClick();
		this.press();
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.focused() && (input.isEnter() || input.plain(Key.SPACE))) {
			this.playClick();
			this.press();
			return true;
		}
		return false;
	}
}
