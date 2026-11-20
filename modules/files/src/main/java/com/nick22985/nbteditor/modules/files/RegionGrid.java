package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.RegionChunk;
import com.nick22985.nbteditor.nbt.RegionPos;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.RegionUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class RegionGrid extends Widget {
	private static final int SIDE = 32;
	private static final int PRESENT = 0xFF3A8F5A;
	private static final int UNREADABLE = 0xFF8F3A3A;
	private static final int EMPTY = 0x18FFFFFF;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

	private final ModuleContext ctx;
	private final RegionPos pos;
	private final RegionChunk[] chunks = new RegionChunk[RegionUtil.CHUNKS];
	private final IntConsumer onPick;

	public RegionGrid(ModuleContext ctx, RegionPos pos, IntConsumer onPick) {
		this.ctx = ctx;
		this.pos = pos;
		this.onPick = onPick;
	}

	public void chunks(List<RegionChunk> loaded) {
		for (int i = 0; i < this.chunks.length; i++) {
			this.chunks[i] = null;
		}
		for (RegionChunk chunk : loaded) {
			this.chunks[chunk.index()] = chunk;
		}
	}

	private int cell() {
		return Math.max(2, Math.min(this.width, this.height) / SIDE);
	}

	private int originX() {
		return this.x + (this.width - this.cell() * SIDE) / 2;
	}

	private int originY() {
		return this.y + (this.height - this.cell() * SIDE) / 2;
	}

	private int indexAt(double mouseX, double mouseY) {
		int cell = this.cell();
		int column = (int) Math.floor((mouseX - this.originX()) / cell);
		int row = (int) Math.floor((mouseY - this.originY()) / cell);
		if (column < 0 || row < 0 || column >= SIDE || row >= SIDE) {
			return -1;
		}
		return RegionUtil.index(column, row);
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		int cell = this.cell();
		int originX = this.originX();
		int originY = this.originY();
		DrawUtil.well(canvas, originX - 2, originY - 2, cell * SIDE + 4, cell * SIDE + 4);
		int hovered = this.contains(mouseX, mouseY) ? this.indexAt(mouseX, mouseY) : -1;
		for (int index = 0; index < RegionUtil.CHUNKS; index++) {
			RegionChunk chunk = this.chunks[index];
			int color = chunk == null ? EMPTY : RegionUtil.readable(chunk) ? PRESENT : UNREADABLE;
			int cellX = originX + (index & 31) * cell;
			int cellY = originY + (index >> 5) * cell;
			DrawUtil.rect(canvas, cellX, cellY, cell - 1, cell - 1, color);
			if (index == hovered) {
				DrawUtil.outline(canvas, cellX - 1, cellY - 1, cell + 1, cell + 1, chunk == null ? Theme.FAINT : Theme.TEXT);
			}
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		int index = this.indexAt(mouseX, mouseY);
		if (index < 0) {
			return null;
		}
		List<Text> lines = new ArrayList<>();
		lines.add(this.ctx.tr("region.chunk", this.pos.chunkX(index), this.pos.chunkZ(index)));
		RegionChunk chunk = this.chunks[index];
		if (chunk == null) {
			lines.add(this.ctx.tr("region.empty").withStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
			return lines;
		}
		if (chunk.timestamp() > 0) {
			lines.add(this.ctx.tr("region.saved", TIME.format(Instant.ofEpochSecond(chunk.timestamp() & 0xFFFFFFFFL))).withStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		}
		if (!RegionUtil.readable(chunk)) {
			lines.add(this.ctx.tr("region.unsupported", chunk.compression()).withStyle(Style.EMPTY.withColor(Formatting.RED)));
		}
		return lines;
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		int index = this.indexAt(input.x(), input.y());
		if (index < 0 || !input.left() || this.chunks[index] == null || !RegionUtil.readable(this.chunks[index])) {
			return false;
		}
		this.playClick();
		this.onPick.accept(index);
		return true;
	}
}
