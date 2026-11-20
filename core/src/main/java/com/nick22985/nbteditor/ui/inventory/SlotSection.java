package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.SlotIcon;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class SlotSection {
	private final Text label;
	private final ContainerLayout layout;
	private final Set<Integer> locked = new HashSet<>();
	private SlotMode mode = SlotMode.NORMAL;

	protected SlotSection(Text label, ContainerLayout layout) {
		this.label = label;
		this.layout = layout;
	}

	public Text label() {
		return this.label;
	}

	public ContainerLayout layout() {
		return this.layout;
	}

	public int size() {
		return this.layout.size();
	}

	public SlotIcon icon(int index) {
		return this.layout.slots().get(index).icon();
	}

	public SlotMode mode() {
		return this.mode;
	}

	public SlotSection mode(SlotMode mode) {
		this.mode = mode;
		return this;
	}

	public boolean locked(int index) {
		return this.locked.contains(index);
	}

	public SlotSection lock(int index) {
		this.locked.add(index);
		return this;
	}

	public void unlockAll() {
		this.locked.clear();
	}

	public boolean writable(int index) {
		return this.mode == SlotMode.NORMAL && !this.locked(index);
	}

	public abstract GameItem get(int index);

	public abstract void set(int index, GameItem item);

	public Target target(int index) {
		return null;
	}
}
