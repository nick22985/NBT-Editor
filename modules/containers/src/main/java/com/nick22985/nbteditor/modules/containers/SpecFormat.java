package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SpecFormat implements ContainerFormat {
	private final Predicate<ContainerSubject> matcher;
	private final ContainerHost host;
	private final Function<ContainerSubject, List<SlotStore>> stores;

	public SpecFormat(Predicate<ContainerSubject> matcher, ContainerHost host, Function<ContainerSubject, List<SlotStore>> stores) {
		this.matcher = matcher;
		this.host = host;
		this.stores = stores;
	}

	public static SpecFormat of(Predicate<ContainerSubject> matcher, ContainerHost host, SlotStore... stores) {
		List<SlotStore> list = List.of(stores);
		return new SpecFormat(matcher, host, subject -> list);
	}

	private StoreContext context(ContainerSubject subject) {
		return new StoreContext(subject, this.host);
	}

	@Override
	public boolean supports(ContainerSubject subject) {
		return this.matcher.test(subject) && !this.stores.apply(subject).isEmpty();
	}

	@Override
	public ContainerLayout layout(ContainerSubject subject) {
		StoreContext context = this.context(subject);
		List<ContainerLayout> layouts = new ArrayList<>();
		for (SlotStore store : this.stores.apply(subject)) {
			layouts.add(store.layout(context));
		}
		return layouts.size() == 1 ? layouts.get(0) : ContainerLayout.beside(layouts);
	}

	@Override
	public List<CompoundTag> read(ContainerSubject subject) {
		StoreContext context = this.context(subject);
		List<CompoundTag> items = new ArrayList<>();
		for (SlotStore store : this.stores.apply(subject)) {
			items.addAll(store.read(context));
		}
		return items;
	}

	@Override
	public void write(ContainerSubject subject, List<CompoundTag> items) {
		StoreContext context = this.context(subject);
		int start = 0;
		for (SlotStore store : this.stores.apply(subject)) {
			int size = store.layout(context).size();
			int end = Math.min(items.size(), start + size);
			List<CompoundTag> part = new ArrayList<>(start < end ? items.subList(start, end) : List.of());
			while (part.size() < size) {
				part.add(new CompoundTag());
			}
			store.write(context, part);
			start += size;
		}
	}
}
