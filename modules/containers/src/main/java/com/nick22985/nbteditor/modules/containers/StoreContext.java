package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record StoreContext(ContainerSubject subject, ContainerHost host) {
	public CompoundTag data() {
		return this.host.read(this.subject);
	}

	public void commit(CompoundTag data) {
		this.host.write(this.subject, data);
	}

	public GameVersion version() {
		return this.subject.version();
	}

	public boolean item() {
		return this.subject.item();
	}

	public boolean containerComponent() {
		return this.host.containerComponent(this.subject);
	}
}
