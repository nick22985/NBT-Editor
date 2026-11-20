package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.text.Text;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class UiScreen extends WidgetGroup {
	private final Text title;
	private final List<Overlay> overlays = new ArrayList<>();
	private UiHost host;
	private Widget focused;
	private Widget captured;
	private Widget popup;
	private boolean built;

	protected UiScreen(Text title) {
		this.title = title;
	}

	public Text title() {
		return this.title;
	}

	@Override
	public UiHost host() {
		return this.host;
	}

	@Override
	public UiScreen screen() {
		return this;
	}

	public final void attach(UiHost host, int width, int height) {
		this.host = host;
		this.resize(width, height);
	}

	public final void resize(int width, int height) {
		Widget previousFocus = this.focused;
		this.bounds(0, 0, width, height);
		this.clear();
		this.focused = null;
		this.captured = null;
		this.popup = null;
		this.build();
		this.built = true;
		for (Overlay overlay : this.overlays) {
			overlay.layout(width, height);
		}
		if (previousFocus != null && this.isAncestorOf(previousFocus)) {
			this.focused = previousFocus;
		}
		this.afterBuild();
	}

	public boolean built() {
		return this.built;
	}

	protected abstract void build();

	protected void afterBuild() {
	}

	public void rebuild() {
		if (this.host != null) {
			this.resize(this.width, this.height);
		}
	}

	public boolean vanillaBackground() {
		return true;
	}

	public boolean pausesGame() {
		return false;
	}

	public boolean closeOnEscape() {
		return true;
	}

	public void close() {
		if (this.host != null) {
			this.host.close(this);
		}
	}

	public void onRemoved() {
	}

	public void onShown() {
	}

	public boolean filesDropped(List<Path> paths) {
		return false;
	}

	public List<Overlay> overlays() {
		return Collections.unmodifiableList(this.overlays);
	}

	public Overlay topOverlay() {
		return this.overlays.isEmpty() ? null : this.overlays.get(this.overlays.size() - 1);
	}

	public <T extends Overlay> T openOverlay(T overlay) {
		overlay.attachScreen(this);
		this.overlays.add(overlay);
		this.focused = null;
		this.captured = null;
		overlay.layout(this.width, this.height);
		overlay.onOpened();
		return overlay;
	}

	public void closeOverlay(Overlay overlay) {
		if (this.overlays.remove(overlay)) {
			if (this.focused != null && overlay.isAncestorOf(this.focused)) {
				this.focused = null;
			}
			if (this.captured != null && overlay.isAncestorOf(this.captured)) {
				this.captured = null;
			}
			overlay.onClosed();
		}
	}

	public void closeAllOverlays() {
		for (Overlay overlay : List.copyOf(this.overlays)) {
			this.closeOverlay(overlay);
		}
	}

	public Widget popup() {
		return this.popup;
	}

	public void showPopup(Widget popup) {
		popup.attachTo(this);
		this.popup = popup;
	}

	public void hidePopup(Widget popup) {
		if (this.popup == popup) {
			this.popup = null;
		}
	}

	public HoveredItem hoveredItem() {
		return null;
	}

	public boolean typing() {
		Widget focused = this.focusedWidget();
		return focused != null && focused.acceptsText();
	}

	public Widget focusedWidget() {
		return this.focused;
	}

	public void setFocus(Widget widget) {
		if (widget == this.focused) {
			return;
		}
		Widget previous = this.focused;
		this.focused = widget;
		if (previous != null) {
			previous.focusChanged(false);
		}
		if (widget != null) {
			widget.focusChanged(true);
		}
	}

	public void capture(Widget widget) {
		this.captured = widget;
	}

	void forget(Widget widget) {
		if (widget == null) {
			return;
		}
		if (this.focused != null && (this.focused == widget || widget instanceof WidgetGroup group && group.isAncestorOf(this.focused))) {
			this.focused = null;
		}
		if (this.captured != null && (this.captured == widget || widget instanceof WidgetGroup group && group.isAncestorOf(this.captured))) {
			this.captured = null;
		}
	}

	private WidgetGroup activeLayer() {
		Overlay top = this.topOverlay();
		return top == null ? this : top;
	}

	private Widget hit(double mouseX, double mouseY) {
		if (this.popup != null && this.popup.visible() && this.popup.contains(mouseX, mouseY)) {
			return this.popup;
		}
		Overlay top = this.topOverlay();
		if (top != null) {
			return top.widgetAt(mouseX, mouseY);
		}
		return super.widgetAt(mouseX, mouseY);
	}

	@Override
	public Widget widgetAt(double mouseX, double mouseY) {
		return this.hit(mouseX, mouseY);
	}

	private double[] offsetOf(Widget widget) {
		double dx = 0;
		double dy = 0;
		for (WidgetGroup parent = widget.parent(); parent != null; parent = parent.parent()) {
			dx += parent.contentOffsetX();
			dy += parent.contentOffsetY();
		}
		return new double[] {dx, dy};
	}

	public MouseInput localize(Widget widget, MouseInput input) {
		double[] offset = offsetOf(widget);
		return input.offset(offset[0], offset[1]);
	}

	public double[] localize(Widget widget, double mouseX, double mouseY) {
		double[] offset = offsetOf(widget);
		return new double[] {mouseX + offset[0], mouseY + offset[1]};
	}

	public boolean dispatchClick(MouseInput input) {
		this.captured = null;
		Widget target = this.hit(input.x(), input.y());
		if (target != null && target == this.popup) {
			return target.mouseClicked(input);
		}
		Widget focusTarget = null;
		for (Widget current = target; current != null; current = current.parent()) {
			if (current.focusable() && current.interactive()) {
				focusTarget = current;
				break;
			}
		}
		this.setFocus(focusTarget);
		for (Widget current = target; current != null; current = current.parent()) {
			if (current != this && !current.interactive()) {
				continue;
			}
			if (current == this && this.topOverlay() != null) {
				break;
			}
			MouseInput local = this.localize(current, input);
			if (current.mouseClicked(local)) {
				if (this.captured == null) {
					this.captured = current;
				}
				return true;
			}
		}
		return false;
	}

	public boolean dispatchRelease(MouseInput input) {
		Widget target = this.captured;
		this.captured = null;
		if (target != null) {
			return target.mouseReleased(this.localize(target, input));
		}
		return false;
	}

	public boolean dispatchDrag(MouseInput input, double deltaX, double deltaY) {
		Widget target = this.captured;
		if (target != null) {
			return target.mouseDragged(this.localize(target, input), deltaX, deltaY);
		}
		return false;
	}

	public boolean dispatchScroll(double mouseX, double mouseY, double deltaX, double deltaY) {
		Widget target = this.hit(mouseX, mouseY);
		for (Widget current = target; current != null; current = current.parent()) {
			if (current == this && this.topOverlay() != null) {
				break;
			}
			if (current != this && !current.interactive()) {
				continue;
			}
			double[] local = this.localize(current, mouseX, mouseY);
			if (current.mouseScrolled(local[0], local[1], deltaX, deltaY)) {
				return true;
			}
		}
		return false;
	}

	public boolean dispatchKey(KeyInput input) {
		Overlay top = this.topOverlay();
		Widget start = this.focused != null ? this.focused : this.activeLayer();
		for (Widget current = start; current != null; current = current.parent()) {
			if (current == this && top != null) {
				break;
			}
			if (current.keyPressed(input)) {
				return true;
			}
		}
		if (input.is(Key.ESCAPE)) {
			if (top != null) {
				if (top.closeOnEscape()) {
					top.close();
				}
				return true;
			}
			if (this.focused != null && this.focused.focusable() && this.unfocusOnEscape()) {
				this.setFocus(null);
				return true;
			}
			if (this.closeOnEscape()) {
				this.close();
				return true;
			}
			return false;
		}
		if (input.key() == Key.TAB && !input.modifiers().primary()) {
			this.cycleFocus(!input.modifiers().shift());
			return true;
		}
		return false;
	}

	protected boolean unfocusOnEscape() {
		return false;
	}

	public boolean dispatchKeyRelease(KeyInput input) {
		Widget start = this.focused != null ? this.focused : this.activeLayer();
		for (Widget current = start; current != null; current = current.parent()) {
			if (current.keyReleased(input)) {
				return true;
			}
		}
		return false;
	}

	public boolean dispatchChar(int codepoint, Modifiers modifiers) {
		Overlay top = this.topOverlay();
		Widget start = this.focused != null ? this.focused : this.activeLayer();
		for (Widget current = start; current != null; current = current.parent()) {
			if (current == this && top != null) {
				break;
			}
			if (current.charTyped(codepoint, modifiers)) {
				return true;
			}
		}
		return false;
	}

	public void cycleFocus(boolean forward) {
		List<Widget> candidates = new ArrayList<>();
		this.activeLayer().collectFocusable(candidates);
		if (candidates.isEmpty()) {
			return;
		}
		int index = candidates.indexOf(this.focused);
		int next;
		if (index < 0) {
			next = forward ? 0 : candidates.size() - 1;
		} else {
			next = Math.floorMod(index + (forward ? 1 : -1), candidates.size());
		}
		this.setFocus(candidates.get(next));
	}

	@Override
	public void tick() {
		super.tick();
		for (Overlay overlay : List.copyOf(this.overlays)) {
			overlay.tick();
		}
		this.onTick();
	}

	protected void onTick() {
	}

	public void renderScreen(Canvas canvas, int mouseX, int mouseY, float delta) {
		boolean blocked = !this.overlays.isEmpty();
		int baseX = blocked ? Integer.MIN_VALUE / 2 : mouseX;
		int baseY = blocked ? Integer.MIN_VALUE / 2 : mouseY;
		this.render(canvas, baseX, baseY, delta);
		for (int i = 0; i < this.overlays.size(); i++) {
			Overlay overlay = this.overlays.get(i);
			boolean top = i == this.overlays.size() - 1;
			canvas.nextLayer();
			if (overlay.dimBackground()) {
				canvas.fill(0, 0, this.width, this.height, Theme.SHADE);
			}
			overlay.render(canvas, top ? mouseX : Integer.MIN_VALUE / 2, top ? mouseY : Integer.MIN_VALUE / 2, delta);
		}
		if (this.popup != null && this.popup.visible()) {
			canvas.nextLayer();
			this.popup.render(canvas, mouseX, mouseY, delta);
		}
		Widget hovered = this.hit(mouseX, mouseY);
		for (Widget current = hovered; current != null; current = current.parent()) {
			double[] local = this.localize(current, mouseX, mouseY);
			List<Text> lines = current.tooltip((int) Math.floor(local[0]), (int) Math.floor(local[1]));
			if (lines != null && !lines.isEmpty()) {
				canvas.tooltip(lines, mouseX, mouseY);
				break;
			}
			if (current instanceof Overlay) {
				break;
			}
		}
	}
}
