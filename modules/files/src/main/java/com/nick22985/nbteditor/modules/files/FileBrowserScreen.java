package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.PanelScreen;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Breadcrumbs;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.RowList;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ImageLoreUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.NbtFileUtil;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FileBrowserScreen extends PanelScreen {
	private static final int ROW_HEIGHT = 16;
	private static final int CHECK_WIDTH = 110;
	private static final int CRUMB_HEIGHT = 12;
	private static final String PARENT = "..";

	private final FileBrowser browser;
	private final ModuleContext ctx;
	private Path dir;
	private List<FileEntry> entries = List.of();
	private Text listError;
	private String query = "";
	private boolean nbtOnly = true;
	private RowList list;

	public FileBrowserScreen(FileBrowser browser, Path dir) {
		super(browser.ctx().tr("browser.title"));
		this.browser = browser;
		this.ctx = browser.ctx();
		this.dir = dir.toAbsolutePath().normalize();
		this.load();
	}

	public Path dir() {
		return this.dir;
	}

	@Override
	protected Text subtitle() {
		return this.ctx.tr("browser.subtitle", this.shownEntries().size());
	}

	@Override
	protected void buildPanel() {
		Breadcrumbs crumbs = this.add(new Breadcrumbs());
		List<Path> chain = this.chain();
		List<Text> labels = new ArrayList<>();
		for (Path path : chain) {
			Path name = path.getFileName();
			labels.add(Text.literal(name == null ? path.toString() : name.toString()));
		}
		crumbs.crumbs(labels).onSelect(index -> this.navigate(chain.get(index)));
		crumbs.bounds(this.bodyX, this.bodyY, this.bodyWidth, CRUMB_HEIGHT);
		int searchY = this.bodyY + CRUMB_HEIGHT + Theme.GAP;
		TextField search = this.add(new TextField(this.query));
		search.placeholder(this.ctx.tr("browser.search"));
		search.onChange(value -> {
			this.query = value;
			this.populate();
		});
		search.onSubmit(this::submit);
		search.bounds(this.bodyX, searchY, this.bodyWidth - CHECK_WIDTH - Theme.GAP, Theme.FIELD_HEIGHT);
		this.add(new Checkbox(this.ctx.tr("browser.nbt_only"), this.nbtOnly, value -> {
			this.nbtOnly = value;
			this.populate();
		})).bounds(this.bodyX + this.bodyWidth - CHECK_WIDTH, searchY + (Theme.FIELD_HEIGHT - Theme.BUTTON_HEIGHT) / 2, CHECK_WIDTH, Theme.BUTTON_HEIGHT);
		int listY = searchY + Theme.FIELD_HEIGHT + Theme.GAP;
		this.list = this.add(new RowList());
		this.list.bounds(this.bodyX, listY, this.bodyWidth, this.bodyY + this.bodyHeight - listY);
		this.doneButton();
		this.footerButton(this.ctx.tr("browser.open_folder"), () -> this.ctx.client().system().openFile(this.dir));
		this.footerButton(this.ctx.tr("browser.exports"), () -> {
			try {
				this.navigate(this.browser.ensureExportsDir());
			} catch (IOException e) {
				ModalUtil.error(this, this.ctx.tr("browser.error"), Text.literal(String.valueOf(e.getMessage())));
			}
		});
		this.footerButton(this.ctx.tr("browser.game_folder"), () -> this.navigate(this.browser.gameDir()));
		this.populate();
		this.setFocus(search);
	}

	private List<Path> chain() {
		List<Path> chain = new ArrayList<>();
		Path path = this.dir;
		while (path != null) {
			chain.add(0, path);
			path = path.getParent();
		}
		return chain;
	}

	private void submit(String value) {
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return;
		}
		Path typed = NbtFileUtil.resolve(this.dir, trimmed);
		if (Files.exists(typed)) {
			this.query = "";
			this.activate(typed);
			return;
		}
		List<FileEntry> visible = this.shownEntries();
		if (visible.size() == 1) {
			this.activate(visible.get(0).path());
		}
	}

	public void navigate(Path dir) {
		this.dir = dir.toAbsolutePath().normalize();
		this.query = "";
		this.browser.remember(this.dir);
		this.load();
		this.rebuild();
	}

	private void load() {
		List<FileEntry> loaded = new ArrayList<>();
		this.listError = null;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.dir)) {
			for (Path path : stream) {
				boolean directory = Files.isDirectory(path);
				String name = path.getFileName().toString();
				loaded.add(new FileEntry(path, name, directory, directory ? -1 : NbtFileUtil.sizeOf(path), directory ? null : NbtFileFormat.of(path), !directory && ImageLoreUtil.isImage(path)));
			}
		} catch (IOException | RuntimeException e) {
			this.listError = this.ctx.tr("browser.unreadable", String.valueOf(e.getMessage()));
		}
		loaded.sort(Comparator.comparing((FileEntry entry) -> !entry.directory()).thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
		this.entries = loaded;
	}

	public List<FileEntry> shownEntries() {
		String wanted = this.query.trim().toLowerCase(Locale.ROOT);
		List<FileEntry> out = new ArrayList<>();
		for (FileEntry entry : this.entries) {
			if (this.nbtOnly && !entry.openable()) {
				continue;
			}
			if (!wanted.isEmpty() && !entry.name().toLowerCase(Locale.ROOT).contains(wanted)) {
				continue;
			}
			out.add(entry);
		}
		return out;
	}

	private void populate() {
		if (this.list == null) {
			return;
		}
		List<Row> rows = new ArrayList<>();
		Path parent = this.dir.getParent();
		if (parent != null && this.query.isEmpty()) {
			rows.add(this.row(new FileEntry(parent, PARENT, true, -1, null, false)));
		}
		List<FileEntry> visible = this.shownEntries();
		if (visible.isEmpty()) {
			Row empty = new Row(ROW_HEIGHT);
			Text message = this.listError != null ? this.listError : this.ctx.tr("browser.empty");
			int color = this.listError != null ? Theme.RED : Theme.FAINT;
			empty.decorate((canvas, self) -> DrawUtil.text(canvas, message, self.x() + 4, self.y() + 4, color));
			rows.add(empty);
		}
		for (FileEntry entry : visible) {
			rows.add(this.row(entry));
		}
		this.list.rows(rows);
	}

	private Row row(FileEntry entry) {
		Row row = new Row(ROW_HEIGHT);
		row.decorate((canvas, self) -> this.drawRow(canvas, self, entry));
		boolean deletable = !entry.directory() && !PARENT.equals(entry.name());
		if (entry.openable() || deletable) {
			row.onClick(input -> {
				if (input.right() && deletable) {
					this.confirmDelete(entry);
				} else if (input.left() && entry.openable()) {
					this.activate(entry.path());
				}
			});
		}
		row.hoverable(entry.openable());
		row.tooltip(() -> deletable ? List.of(Text.literal(entry.path().toString()), this.ctx.tr("browser.delete.hint")) : List.of(Text.literal(entry.path().toString())));
		return row;
	}

	private void confirmDelete(FileEntry entry) {
		ModalUtil.confirmDanger(this, this.ctx.tr("browser.delete.title"), this.ctx.tr("browser.delete.desc", entry.name()), this.ctx.tr("browser.delete"), () -> {
			try {
				Files.delete(entry.path());
			} catch (IOException e) {
				ModalUtil.error(this, this.ctx.tr("browser.delete.failed"), Text.literal(String.valueOf(e.getMessage())));
			}
			this.load();
			this.populate();
		});
	}

	private void drawRow(Canvas canvas, Row self, FileEntry entry) {
		int textY = self.y() + (ROW_HEIGHT - 8) / 2;
		String[] sprite = entry.directory() ? PixelSprites.FOLDER : PixelSprites.FILE;
		int iconColor = entry.directory() ? Theme.GOLD : entry.format() != null ? Theme.AQUA : Theme.DIM;
		DrawUtil.spriteCentered(canvas, sprite, self.x() + 4, self.y(), 8, ROW_HEIGHT, iconColor);
		int right = self.x() + self.width() - 6;
		if (entry.size() >= 0) {
			String size = NbtFileUtil.size(entry.size());
			right -= canvas.textWidth(size);
			DrawUtil.text(canvas, size, right, textY, Theme.FAINT);
			right -= Theme.GAP * 2;
		}
		if (entry.format() != null) {
			String label = this.host().translate(this.ctx.key("format." + entry.format().name().toLowerCase(Locale.ROOT)));
			right -= DrawUtil.chipWidth(canvas, label);
			DrawUtil.chip(canvas, label, right, self.y() + 2, Theme.LABEL);
			right -= Theme.GAP;
		}
		int color = entry.openable() ? Theme.TEXT : Theme.FAINT;
		DrawUtil.textClipped(canvas, entry.name(), self.x() + 18, textY, right - self.x() - 22, color);
	}

	private void activate(Path path) {
		if (Files.isDirectory(path)) {
			this.navigate(path);
			return;
		}
		if (!this.browser.open(path)) {
			ModalUtil.error(this, this.ctx.tr("browser.error"), this.ctx.tr("browser.unsupported", path.getFileName().toString()));
		}
	}

	@Override
	public boolean filesDropped(List<Path> paths) {
		if (paths.size() == 1 && Files.isDirectory(paths.get(0))) {
			this.navigate(paths.get(0));
			return true;
		}
		for (Path path : paths) {
			if (this.browser.open(path)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onShown() {
		this.load();
		this.populate();
	}
}
