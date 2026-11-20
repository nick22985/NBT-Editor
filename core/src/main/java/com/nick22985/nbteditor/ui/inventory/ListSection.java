package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ListSection extends SlotSection {
	private final List<GameItem> items;
	private Runnable onChange = () -> {
	};
	private IntFunction<Target> targets = index -> null;

	public ListSection(Text label, ContainerLayout layout, List<GameItem> items, GameItem empty) {
		super(label, layout);
		this.items = new ArrayList<>(items);
		while (this.items.size() < layout.size()) {
			this.items.add(empty);
		}
	}

	public ListSection onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	public ListSection targets(IntFunction<Target> targets) {
		this.targets = targets;
		return this;
	}

	public List<GameItem> items() {
		return List.copyOf(this.items.subList(0, this.size()));
	}

	public void replace(List<GameItem> items) {
		for (int i = 0; i < this.size() && i < items.size(); i++) {
			this.items.set(i, items.get(i));
		}
	}

	@Override
	public GameItem get(int index) {
		return this.items.get(index);
	}

	@Override
	public void set(int index, GameItem item) {
		this.items.set(index, item);
		this.onChange.run();
	}

	@Override
	public Target target(int index) {
		return this.targets.apply(index);
	}
}
