package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.PanelScreen;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ImageLoreUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.NbtFileUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ImportScreen extends PanelScreen {
	private static final int SIDE_WIDTH = 150;
	private static final int LABEL_HEIGHT = 11;
	private static final int LINE = 10;
	private static final int MAX_LOG = 8;
	private static final String[] KINDS = {"kind.export", "kind.structure", "kind.snbt", "kind.region", "kind.image"};

	private final ModuleContext ctx;
	private final FileBrowser browser;
	private final Importer importer;
	private final List<ImportMessage> log = new ArrayList<>();
	private String dataVersion = "";
	private String imageWidth;
	private String imageHeight = "";
	private TextField versionField;
	private TextField widthField;
	private TextField heightField;
	private int dropWidth;
	private int sideX;
	private int kindsY;

	public ImportScreen(ModuleContext ctx, FileBrowser browser, Importer importer) {
		super(ctx.tr("import.title"));
		this.ctx = ctx;
		this.browser = browser;
		this.importer = importer;
		this.imageWidth = String.valueOf(browser.settings().imageWidth());
	}

	public List<ImportMessage> log() {
		return this.log;
	}

	@Override
	protected Text subtitle() {
		return this.ctx.tr("import.subtitle");
	}

	@Override
	protected void buildPanel() {
		this.dropWidth = this.bodyWidth - SIDE_WIDTH - Theme.GAP * 2;
		this.sideX = this.bodyX + this.dropWidth + Theme.GAP * 2;
		int y = this.bodyY + LABEL_HEIGHT;
		this.versionField = this.add(new TextField(this.dataVersion));
		this.versionField.placeholder(this.ctx.tr("import.auto"));
		this.versionField.validator(value -> NbtFileUtil.parseDataVersion(value) == NbtFileUtil.INVALID ? this.ctx.tr("import.bad_version") : null);
		this.versionField.onChange(value -> this.dataVersion = value);
		this.versionField.bounds(this.sideX, y, SIDE_WIDTH, Theme.FIELD_HEIGHT);
		y += Theme.FIELD_HEIGHT + Theme.GAP + LABEL_HEIGHT;
		this.widthField = this.add(new TextField(this.imageWidth));
		this.widthField.filter(value -> value.chars().allMatch(Character::isDigit));
		this.widthField.maxLength(3);
		this.widthField.placeholder(this.ctx.tr("import.auto"));
		this.widthField.validator(value -> this.size(value) < 0 ? this.ctx.tr("import.bad_size", ImageLoreUtil.MAX_SIZE) : null);
		this.widthField.onChange(value -> this.imageWidth = value);
		int half = (SIDE_WIDTH - Theme.GAP) / 2;
		this.widthField.bounds(this.sideX, y, half, Theme.FIELD_HEIGHT);
		this.heightField = this.add(new TextField(this.imageHeight));
		this.heightField.filter(value -> value.chars().allMatch(Character::isDigit));
		this.heightField.maxLength(3);
		this.heightField.placeholder(this.ctx.tr("import.auto"));
		this.heightField.validator(value -> this.size(value) < 0 ? this.ctx.tr("import.bad_size", ImageLoreUtil.MAX_SIZE) : null);
		this.heightField.onChange(value -> this.imageHeight = value);
		this.heightField.bounds(this.sideX + SIDE_WIDTH - half, y, half, Theme.FIELD_HEIGHT);
		this.kindsY = y + Theme.FIELD_HEIGHT + Theme.GAP * 2;
		this.doneButton();
		this.footerButton(this.ctx.tr("import.browse"), () -> this.browser.browse(this.browser.startDir()));
		this.footerButton(this.ctx.tr("browser.exports"), () -> {
			try {
				this.ctx.client().system().openFile(this.browser.ensureExportsDir());
			} catch (IOException e) {
				ModalUtil.error(this, this.ctx.tr("browser.error"), Text.literal(String.valueOf(e.getMessage())));
			}
		});
	}

	private int size(String value) {
		if (value.trim().isEmpty()) {
			return 0;
		}
		try {
			int size = Integer.parseInt(value.trim());
			return size >= 1 && size <= ImageLoreUtil.MAX_SIZE ? size : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public ImportOptions options() {
		ImportOptions defaults = this.importer.defaults();
		int version = NbtFileUtil.parseDataVersion(this.dataVersion);
		int width = this.size(this.imageWidth);
		int height = this.size(this.imageHeight);
		return new ImportOptions(version > 0 ? version : NbtFileUtil.AUTO, width > 0 || height > 0 ? Math.max(width, 0) : defaults.imageWidth(), Math.max(height, 0), defaults.at());
	}

	@Override
	public boolean filesDropped(List<Path> paths) {
		if (NbtFileUtil.parseDataVersion(this.dataVersion) == NbtFileUtil.INVALID) {
			this.record(new ImportMessage(this.ctx.tr("import.bad_version"), true));
			return true;
		}
		this.importer.importFiles(paths, this.options(), this::record);
		return true;
	}

	private void record(ImportMessage message) {
		this.log.add(0, message);
		while (this.log.size() > MAX_LOG) {
			this.log.remove(this.log.size() - 1);
		}
	}

	@Override
	protected void renderPanelBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		int logHeight = MAX_LOG * LINE + Theme.GAP * 2;
		int dropHeight = this.bodyHeight - logHeight - Theme.GAP;
		DrawUtil.well(canvas, this.bodyX, this.bodyY, this.dropWidth, dropHeight);
		DrawUtil.dashedOutline(canvas, this.bodyX + 4, this.bodyY + 4, this.dropWidth - 8, dropHeight - 8, Theme.FAINT);
		int centerX = this.bodyX + this.dropWidth / 2;
		int centerY = this.bodyY + dropHeight / 2;
		DrawUtil.spriteCentered(canvas, PixelSprites.IMPORT, centerX - 10, centerY - 22, 20, 14, Theme.LABEL);
		DrawUtil.textCentered(canvas, this.ctx.tr("import.drop"), centerX, centerY - 2, Theme.TEXT);
		int hintY = centerY + 10;
		for (Text line : canvas.wrap(this.ctx.tr("import.drop_hint"), this.dropWidth - 24)) {
			DrawUtil.textCentered(canvas, line, centerX, hintY, Theme.FAINT);
			hintY += LINE;
		}
		int logY = this.bodyY + dropHeight + Theme.GAP;
		DrawUtil.well(canvas, this.bodyX, logY, this.dropWidth, logHeight);
		if (this.log.isEmpty()) {
			DrawUtil.text(canvas, this.ctx.tr("import.log_empty"), this.bodyX + 6, logY + Theme.GAP + 1, Theme.FAINT);
		}
		for (int i = 0; i < this.log.size(); i++) {
			ImportMessage message = this.log.get(i);
			DrawUtil.textClipped(canvas, message.text(), this.bodyX + 6, logY + Theme.GAP + 1 + i * LINE, this.dropWidth - 12, message.error() ? Theme.RED : i == 0 ? Theme.TEXT : Theme.LABEL);
		}
		DrawUtil.text(canvas, this.ctx.tr("import.data_version"), this.sideX, this.bodyY + 1, Theme.LABEL);
		DrawUtil.text(canvas, this.ctx.tr("import.image_size"), this.sideX, this.bodyY + LABEL_HEIGHT + Theme.FIELD_HEIGHT + Theme.GAP + 1, Theme.LABEL);
		DrawUtil.sectionHeading(canvas, this.ctx.tr("import.kinds"), this.sideX, this.kindsY, SIDE_WIDTH);
		int y = this.kindsY + 14;
		for (String kind : KINDS) {
			DrawUtil.textClipped(canvas, this.ctx.tr(kind), this.sideX + 2, y, SIDE_WIDTH - 2, Theme.FAINT);
			y += LINE + 1;
		}
		DrawUtil.textClipped(canvas, this.ctx.tr("import.version_note"), this.sideX + 2, y + Theme.GAP, SIDE_WIDTH - 2, Theme.DIM);
	}
}
