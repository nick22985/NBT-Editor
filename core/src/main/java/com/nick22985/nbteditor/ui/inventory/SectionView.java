package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.container.ContainerSlotDef;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.SlotIcon;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SectionView extends Widget {
	private final InventoryScreen owner;
	private final SlotSection section;
	private int labelY;

	public SectionView(InventoryScreen owner, SlotSection section) {
		this.owner = owner;
		this.section = section;
		this.width = section.layout().width();
		this.height = section.layout().height();
	}

	public SlotSection section() {
		return this.section;
	}

	public int labelY() {
		return this.labelY;
	}

	public SectionView labelY(int labelY) {
		this.labelY = labelY;
		return this;
	}

	public int indexAt(double mouseX, double mouseY) {
		if (!this.contains(mouseX, mouseY)) {
			return -1;
		}
		List<ContainerSlotDef> slots = this.section.layout().slots();
		for (int i = 0; i < slots.size(); i++) {
			ContainerSlotDef slot = slots.get(i);
			if (DrawUtil.inside(mouseX, mouseY, this.x + slot.x(), this.y + slot.y(), Theme.SLOT, Theme.SLOT)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		List<ContainerSlotDef> slots = this.section.layout().slots();
		for (int i = 0; i < slots.size(); i++) {
			ContainerSlotDef slot = slots.get(i);
			int slotX = this.x + slot.x();
			int slotY = this.y + slot.y();
			canvas.slot(slotX, slotY);
			GameItem item = this.section.get(i);
			if (InventoryUtil.present(item)) {
				canvas.item(item, slotX + 1, slotY + 1);
			} else if (slot.icon() != SlotIcon.NONE) {
				canvas.slotIcon(slot.icon(), slotX, slotY);
			}
		}
		canvas.nextLayer();
		for (int i = 0; i < slots.size(); i++) {
			ContainerSlotDef slot = slots.get(i);
			if (this.section.locked(i)) {
				DrawUtil.slotHighlight(canvas, this.x + slot.x() + 1, this.y + slot.y() + 1, Theme.LOCKED_SLOT);
			} else if (this.owner.dragging(this.section, i)) {
				DrawUtil.slotHighlight(canvas, this.x + slot.x() + 1, this.y + slot.y() + 1, Theme.SLOT_HIGHLIGHT);
			}
		}
		int hovered = this.interactive() ? this.indexAt(mouseX, mouseY) : -1;
		if (hovered >= 0) {
			ContainerSlotDef slot = slots.get(hovered);
			DrawUtil.slotHighlight(canvas, this.x + slot.x() + 1, this.y + slot.y() + 1, Theme.SLOT_HIGHLIGHT);
		}
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		int index = this.indexAt(input.x(), input.y());
		if (index < 0) {
			return false;
		}
		this.owner.press(new SlotRef(this.section, index), input);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		this.owner.dragOver(input.x(), input.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseInput input) {
		this.owner.release();
		return true;
	}
}
