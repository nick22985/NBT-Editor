package com.nick22985.nbteditor.container;

import com.nick22985.nbteditor.ui.SlotIcon;
import com.nick22985.nbteditor.ui.Theme;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ContainerLayout(int width, int height, List<ContainerSlotDef> slots) {
	public static final int SECTION_GAP = 6;

	public ContainerLayout {
		slots = List.copyOf(slots);
	}

	public static ContainerLayout of(List<ContainerSlotDef> slots) {
		int width = 0;
		int height = 0;
		for (ContainerSlotDef slot : slots) {
			width = Math.max(width, slot.x() + Theme.SLOT);
			height = Math.max(height, slot.y() + Theme.SLOT);
		}
		return new ContainerLayout(width, height, slots);
	}

	public static ContainerLayout grid(int columns, int rows) {
		List<ContainerSlotDef> slots = new ArrayList<>();
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				slots.add(ContainerSlotDef.of(column * Theme.SLOT, row * Theme.SLOT));
			}
		}
		return of(slots);
	}

	public static ContainerLayout row(List<SlotIcon> icons) {
		List<ContainerSlotDef> slots = new ArrayList<>();
		for (int i = 0; i < icons.size(); i++) {
			slots.add(new ContainerSlotDef(i * Theme.SLOT, 0, icons.get(i)));
		}
		return of(slots);
	}

	public static ContainerLayout stack(List<ContainerLayout> sections) {
		return stack(SECTION_GAP, sections);
	}

	public static ContainerLayout stack(int gap, List<ContainerLayout> sections) {
		List<ContainerSlotDef> slots = new ArrayList<>();
		int y = 0;
		for (ContainerLayout section : sections) {
			if (section.size() == 0) {
				continue;
			}
			for (ContainerSlotDef slot : section.slots()) {
				slots.add(new ContainerSlotDef(slot.x(), slot.y() + y, slot.icon()));
			}
			y += section.height() + gap;
		}
		return of(slots);
	}

	public static ContainerLayout beside(List<ContainerLayout> sections) {
		return beside(SECTION_GAP, sections);
	}

	public static ContainerLayout beside(int gap, List<ContainerLayout> sections) {
		List<ContainerSlotDef> slots = new ArrayList<>();
		int x = 0;
		for (ContainerLayout section : sections) {
			if (section.size() == 0) {
				continue;
			}
			for (ContainerSlotDef slot : section.slots()) {
				slots.add(new ContainerSlotDef(slot.x() + x, slot.y(), slot.icon()));
			}
			x += section.width() + gap;
		}
		return of(slots);
	}

	public static ContainerLayout column(List<SlotIcon> icons) {
		List<ContainerSlotDef> slots = new ArrayList<>();
		for (int i = 0; i < icons.size(); i++) {
			slots.add(new ContainerSlotDef(0, i * Theme.SLOT, icons.get(i)));
		}
		return of(slots);
	}

	public int size() {
		return this.slots.size();
	}
}
