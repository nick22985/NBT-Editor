package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NamedTag;
import com.nick22985.nbteditor.nbt.NbtCompression;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.NbtFileUtil;
import com.nick22985.nbteditor.util.NbtIoUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FileTarget implements Target {
	private final ModuleContext ctx;
	private final Path path;
	private final NbtFileFormat format;
	private NbtCompression compression = NbtCompression.GZIP;
	private String rootName = "";
	private GameVersion dataVersion;

	public FileTarget(ModuleContext ctx, Path path, NbtFileFormat format) {
		this.ctx = ctx;
		this.path = path;
		this.format = format;
	}

	public Path path() {
		return this.path;
	}

	@Override
	public String id() {
		return "file:" + this.path.toAbsolutePath();
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.FILE;
	}

	@Override
	public Text displayName() {
		return Text.literal(this.path.getFileName().toString());
	}

	@Override
	public Text source() {
		return this.ctx.tr("target.file", this.path.toAbsolutePath().toString());
	}

	@Override
	public Set<TargetCapability> capabilities() {
		return EnumSet.of(TargetCapability.READ, TargetCapability.WRITE);
	}

	@Override
	public GameVersion dataVersion() {
		return this.dataVersion;
	}

	private SnbtDialect dialect() {
		GameVersion version = this.dataVersion == null ? this.ctx.platform().game().version() : this.dataVersion;
		return version.has(Feature.MODERN_SNBT) ? SnbtDialect.MODERN : SnbtDialect.LEGACY;
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		return this.ctx.platform().scheduler().async(() -> {
			try {
				CompoundTag tag = this.read();
				if (tag == null) {
					throw new TargetException(this.ctx.tr("file.not_compound", this.path.getFileName().toString()));
				}
				this.dataVersion = NbtFileUtil.dataVersion(tag);
				return tag;
			} catch (IOException | SnbtException e) {
				throw new TargetException(this.ctx.tr("file.read_failed", this.path.getFileName().toString(), String.valueOf(e.getMessage())), e);
			}
		});
	}

	private CompoundTag read() throws IOException, SnbtException {
		if (this.format == NbtFileFormat.SNBT) {
			return SnbtUtil.parseCompound(Files.readString(this.path, StandardCharsets.UTF_8), this.dialect());
		}
		byte[] bytes = Files.readAllBytes(this.path);
		this.compression = NbtCompression.detect(bytes);
		NamedTag named = NbtIoUtil.readAuto(bytes);
		this.rootName = named.name();
		return named.tag() instanceof CompoundTag ? (CompoundTag) named.tag() : null;
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		CompoundTag copy = tag.copy();
		return this.ctx.platform().scheduler().async(() -> {
			try {
				byte[] data;
				if (this.format == NbtFileFormat.SNBT) {
					data = (SnbtUtil.pretty(copy, this.dialect()) + "\n").getBytes(StandardCharsets.UTF_8);
				} else {
					data = NbtIoUtil.write(new NamedTag(this.rootName, copy), this.compression);
				}
				DataFileUtil.writeAtomic(this.path, data);
				return null;
			} catch (IOException e) {
				throw new TargetException(this.ctx.tr("file.write_failed", this.path.getFileName().toString(), String.valueOf(e.getMessage())), e);
			}
		});
	}
}
