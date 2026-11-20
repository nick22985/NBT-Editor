package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.Objects;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HoverEvent {
	private final HoverAction action;
	private final Text text;
	private final String id;
	private final int count;
	private final CompoundTag data;
	private final UUID uuid;

	private HoverEvent(HoverAction action, Text text, String id, int count, CompoundTag data, UUID uuid) {
		this.action = action;
		this.text = text;
		this.id = id;
		this.count = count;
		this.data = data;
		this.uuid = uuid;
	}

	public static HoverEvent showText(Text text) {
		return new HoverEvent(HoverAction.SHOW_TEXT, Objects.requireNonNull(text), null, 0, null, null);
	}

	public static HoverEvent showItem(String id, int count, CompoundTag data) {
		return new HoverEvent(HoverAction.SHOW_ITEM, null, Objects.requireNonNull(id), count, data, null);
	}

	public static HoverEvent showEntity(String type, UUID uuid, Text name) {
		return new HoverEvent(HoverAction.SHOW_ENTITY, name, Objects.requireNonNull(type), 0, null, Objects.requireNonNull(uuid));
	}

	public HoverAction action() {
		return action;
	}

	public Text text() {
		return text;
	}

	public String id() {
		return id;
	}

	public int count() {
		return count;
	}

	public CompoundTag data() {
		return data;
	}

	public UUID uuid() {
		return uuid;
	}

	public boolean isEmptyItem() {
		return action == HoverAction.SHOW_ITEM && (count <= 0 || id.equals("minecraft:air") || id.equals("air"));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof HoverEvent)) {
			return false;
		}
		HoverEvent other = (HoverEvent) o;
		return action == other.action && Objects.equals(text, other.text) && Objects.equals(id, other.id) && count == other.count && Objects.equals(data, other.data) && Objects.equals(uuid, other.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(action, text, id, count, data, uuid);
	}
}
