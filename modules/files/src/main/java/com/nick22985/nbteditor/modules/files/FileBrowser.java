package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.RegionPos;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.NbtFileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FileBrowser {
	public static final String EXPORTS = "exports";

	private final ModuleContext ctx;
	private final FilesSettings settings;

	public FileBrowser(ModuleContext ctx, FilesSettings settings) {
		this.ctx = ctx;
		this.settings = settings;
	}

	public ModuleContext ctx() {
		return this.ctx;
	}

	public FilesSettings settings() {
		return this.settings;
	}

	public Path gameDir() {
		return this.ctx.platform().game().gameDir().toAbsolutePath().normalize();
	}

	public Path exportsDir() {
		return DataFileUtil.dataDir(this.gameDir()).resolve(EXPORTS);
	}

	public Path ensureExportsDir() throws IOException {
		return Files.createDirectories(this.exportsDir());
	}

	public Path startDir() {
		String last = this.settings.lastFolder();
		if (!last.isEmpty()) {
			Path path = NbtFileUtil.resolve(this.gameDir(), last);
			if (Files.isDirectory(path)) {
				return path;
			}
		}
		return this.gameDir();
	}

	public void remember(Path dir) {
		this.settings.lastFolder(dir.toAbsolutePath().normalize().toString());
	}

	public Path resolve(String input) {
		return NbtFileUtil.resolve(this.gameDir(), input);
	}

	public void browse(Path dir) {
		this.ctx.client().screens().open(new FileBrowserScreen(this, dir));
	}

	public boolean open(Path path) {
		if (Files.isDirectory(path)) {
			this.browse(path);
			return true;
		}
		NbtFileFormat format = NbtFileFormat.of(path);
		if (format == null || !Files.isRegularFile(path)) {
			return false;
		}
		if (format == NbtFileFormat.REGION) {
			this.ctx.client().screens().open(new RegionScreen(this, path));
			return true;
		}
		Optional<EditorService> editor = this.ctx.services().find(EditorService.class);
		if (editor.isEmpty()) {
			return false;
		}
		editor.get().open(new FileTarget(this.ctx, path, format));
		return true;
	}

	public void openChunk(Path region, int index) {
		this.ctx.services().find(EditorService.class).ifPresent(editor -> editor.open(new RegionChunkTarget(this.ctx, region, index)));
	}

	public boolean isRegion(Path path) {
		return NbtFileFormat.of(path) == NbtFileFormat.REGION && RegionPos.parse(path.getFileName().toString()) != null;
	}
}
