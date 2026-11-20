package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.ImageLoreUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FilesSettings {
	private final Setting<String> lastFolder;
	private final Setting<Integer> imageWidth;
	private final Setting<Boolean> dropImport;

	public FilesSettings(ModuleContext ctx) {
		this.lastFolder = ctx.config().string("last_folder", "").hidden();
		this.imageWidth = ctx.config().integer("image_width", ImageLoreUtil.DEFAULT_WIDTH, 1, ImageLoreUtil.MAX_SIZE);
		this.dropImport = ctx.config().bool("drop_import", true);
	}

	public String lastFolder() {
		return this.lastFolder.get();
	}

	public void lastFolder(String folder) {
		this.lastFolder.set(folder);
	}

	public int imageWidth() {
		return this.imageWidth.get();
	}

	public boolean dropImport() {
		return this.dropImport.get();
	}
}
