package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.RegionChunk;
import com.nick22985.nbteditor.nbt.RegionPos;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.NbtFileUtil;
import com.nick22985.nbteditor.util.RegionUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RegionChunkTarget implements Target {
	private final ModuleContext ctx;
	private final Path file;
	private final int index;
	private final RegionPos pos;
	private int compression = RegionUtil.ZLIB;
	private GameVersion dataVersion;

	public RegionChunkTarget(ModuleContext ctx, Path file, int index) {
		this.ctx = ctx;
		this.file = file;
		this.index = index;
		RegionPos parsed = RegionPos.parse(file.getFileName().toString());
		this.pos = parsed == null ? new RegionPos(0, 0) : parsed;
	}

	@Override
	public String id() {
		return "region:" + this.file.toAbsolutePath() + "/" + this.index;
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.FILE;
	}

	@Override
	public Text displayName() {
		return this.ctx.tr("region.chunk", this.pos.chunkX(this.index), this.pos.chunkZ(this.index));
	}

	@Override
	public Text source() {
		return Text.literal(this.file.getFileName().toString());
	}

	@Override
	public Set<TargetCapability> capabilities() {
		return EnumSet.of(TargetCapability.READ, TargetCapability.WRITE);
	}

	@Override
	public GameVersion dataVersion() {
		return this.dataVersion;
	}

	private RegionChunk find(List<RegionChunk> chunks) {
		for (RegionChunk chunk : chunks) {
			if (chunk.index() == this.index) {
				return chunk;
			}
		}
		return null;
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		return this.ctx.platform().scheduler().async(() -> {
			try {
				RegionChunk chunk = this.find(RegionUtil.read(this.file));
				if (chunk == null) {
					throw new TargetException(this.ctx.tr("region.missing"));
				}
				if (!RegionUtil.readable(chunk)) {
					throw new TargetException(this.ctx.tr("region.unsupported", chunk.compression()));
				}
				this.compression = chunk.compression();
				CompoundTag tag = RegionUtil.decode(chunk);
				this.dataVersion = NbtFileUtil.dataVersion(tag);
				return tag;
			} catch (IOException e) {
				throw new TargetException(this.ctx.tr("file.read_failed", this.file.getFileName().toString(), String.valueOf(e.getMessage())), e);
			}
		});
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		CompoundTag copy = tag.copy();
		return this.ctx.platform().scheduler().async(() -> {
			try {
				List<RegionChunk> chunks = RegionUtil.read(this.file);
				int timestamp = (int) (System.currentTimeMillis() / 1000L);
				RegionUtil.write(this.file, RegionUtil.replace(chunks, RegionUtil.encode(this.index, timestamp, this.compression, copy)));
				return null;
			} catch (IOException e) {
				throw new TargetException(this.ctx.tr("file.write_failed", this.file.getFileName().toString(), String.valueOf(e.getMessage())), e);
			}
		});
	}
}
