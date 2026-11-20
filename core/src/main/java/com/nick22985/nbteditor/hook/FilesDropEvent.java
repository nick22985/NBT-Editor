package com.nick22985.nbteditor.hook;

import java.nio.file.Path;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FilesDropEvent extends CancellableEvent {
	private final List<Path> paths;
	private final DropScreen screen;

	public FilesDropEvent(List<Path> paths, DropScreen screen) {
		this.paths = List.copyOf(paths);
		this.screen = screen;
	}

	public List<Path> paths() {
		return this.paths;
	}

	public DropScreen screen() {
		return this.screen;
	}
}
