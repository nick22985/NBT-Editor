package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.RegionChunk;
import com.nick22985.nbteditor.nbt.RegionPos;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.PanelScreen;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.RegionUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RegionScreen extends PanelScreen {
	private final FileBrowser browser;
	private final ModuleContext ctx;
	private final Path file;
	private final RegionPos pos;
	private List<RegionChunk> chunks;
	private Text error;
	private RegionGrid grid;

	public RegionScreen(FileBrowser browser, Path file) {
		super(Text.literal(file.getFileName().toString()));
		this.browser = browser;
		this.ctx = browser.ctx();
		this.file = file;
		RegionPos parsed = RegionPos.parse(file.getFileName().toString());
		this.pos = parsed == null ? new RegionPos(0, 0) : parsed;
	}

	public List<RegionChunk> chunks() {
		return this.chunks;
	}

	@Override
	protected Text subtitle() {
		if (this.error != null) {
			return this.error;
		}
		if (this.chunks == null) {
			return this.ctx.tr("region.loading");
		}
		return this.ctx.tr("region.subtitle", this.chunks.size(), this.pos.x(), this.pos.z());
	}

	@Override
	protected void buildPanel() {
		this.grid = this.add(new RegionGrid(this.ctx, this.pos, index -> this.browser.openChunk(this.file, index)));
		this.grid.bounds(this.bodyX, this.bodyY, this.bodyWidth, this.bodyHeight);
		if (this.chunks != null) {
			this.grid.chunks(this.chunks);
		}
		this.doneButton();
		this.footerButton(this.ctx.tr("region.reload"), this::load);
		this.footerButton(this.ctx.tr("region.folder"), () -> this.browser.browse(this.file.toAbsolutePath().getParent()));
	}

	@Override
	public void onShown() {
		this.load();
	}

	private void load() {
		this.ctx.platform().scheduler().async(() -> {
			try {
				return RegionUtil.read(this.file);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).whenComplete((loaded, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				this.error = this.ctx.tr("file.read_failed", this.file.getFileName().toString(), ErrorUtil.message(failure));
				return;
			}
			this.error = null;
			this.chunks = loaded;
			if (this.grid != null) {
				this.grid.chunks(loaded);
			}
		}));
	}

	@Override
	protected void renderPanelBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		super.renderPanelBackground(canvas, mouseX, mouseY, delta);
		if (this.chunks == null && this.error == null) {
			DrawUtil.textCentered(canvas, this.ctx.tr("region.loading"), this.bodyX + this.bodyWidth / 2, this.bodyY + this.bodyHeight / 2 - 4, Theme.FAINT);
		}
	}
}
