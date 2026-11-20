package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.ModelEntry;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ModelFolder(String pack, String path) {
	public static final ModelFolder ALL = new ModelFolder(null, "");

	public boolean all() {
		return this.pack == null;
	}

	public boolean contains(ModelEntry entry) {
		if (this.pack == null) {
			return true;
		}
		if (!this.pack.equals(entry.pack())) {
			return false;
		}
		return this.path.isEmpty() || entry.folder().equals(this.path) || entry.folder().startsWith(this.path + "/");
	}

	public ModelFolder child(String name) {
		if (this.pack == null) {
			return new ModelFolder(name, "");
		}
		return new ModelFolder(this.pack, this.path.isEmpty() ? name : this.path + "/" + name);
	}

	public String name() {
		if (this.pack == null || this.path.isEmpty()) {
			return this.pack;
		}
		return this.path.substring(this.path.lastIndexOf('/') + 1);
	}
}
