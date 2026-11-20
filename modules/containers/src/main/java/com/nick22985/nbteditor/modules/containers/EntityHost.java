package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ContainerUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EntityHost implements ContainerHost {
	public static final String ITEM_COMPONENT = "entity_data";
	public static final String ITEM_LEGACY = "EntityTag";

	@Override
	public CompoundTag read(ContainerSubject subject) {
		if (!subject.item()) {
			return subject.root().copy();
		}
		Tag tag = Feature.ITEM_COMPONENTS.in(subject.version()) ? ItemStackUtil.component(subject.root(), ITEM_COMPONENT) : ItemDataUtil.dataTag(subject.root(), subject.version(), ITEM_LEGACY);
		return tag instanceof CompoundTag ? ((CompoundTag) tag).copy() : new CompoundTag();
	}

	@Override
	public void write(ContainerSubject subject, CompoundTag data) {
		if (!subject.item()) {
			ContainerUtil.replace(subject.root(), data);
			return;
		}
		ContainerUtil.pruneEmpty(data);
		boolean empty = ContainerUtil.hostEmpty(data);
		if (Feature.ITEM_COMPONENTS.in(subject.version())) {
			if (!empty && !data.contains("id") && subject.entity() != null) {
				data.putAt(0, "id", StringTag.of(subject.entity()));
			}
			ItemStackUtil.setComponent(subject.root(), ITEM_COMPONENT, empty ? null : data);
		} else {
			ItemDataUtil.putData(subject.root(), subject.version(), ITEM_LEGACY, empty ? null : data);
		}
	}
}
