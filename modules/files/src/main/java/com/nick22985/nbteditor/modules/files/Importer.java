package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.data.ExportData;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.DoubleTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.platform.DataFixService;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.Position;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.target.WorldBackends;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockDocUtil;
import com.nick22985.nbteditor.util.BlockStateUtil;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ExportUtil;
import com.nick22985.nbteditor.util.ImageLoreUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.NbtFileUtil;
import com.nick22985.nbteditor.util.PickItemUtil;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Importer {
	public static final String PAINTING = "minecraft:painting";

	private final ModuleContext ctx;
	private final FileBrowser browser;

	public Importer(ModuleContext ctx, FileBrowser browser) {
		this.ctx = ctx;
		this.browser = browser;
	}

	public ImportOptions defaults() {
		return new ImportOptions(NbtFileUtil.AUTO, this.browser.settings().imageWidth(), 0, this.ctx.client().player().position());
	}

	public boolean importable(Path path) {
		return ImageLoreUtil.isImage(path) || NbtFileFormat.of(path) != null;
	}

	public void importFiles(List<Path> paths, ImportOptions options, Consumer<ImportMessage> report) {
		for (Path path : paths) {
			this.importFile(path, options, report);
		}
	}

	public void importFile(Path path, ImportOptions options, Consumer<ImportMessage> report) {
		String name = path.getFileName().toString();
		if (ImageLoreUtil.isImage(path)) {
			this.importImage(path, options, report);
			return;
		}
		NbtFileFormat format = NbtFileFormat.of(path);
		if (format == null) {
			report.accept(this.error(this.ctx.tr("import.unsupported", name)));
			return;
		}
		if (format != NbtFileFormat.NBT) {
			this.openFile(path, report);
			return;
		}
		this.ctx.platform().scheduler().async(() -> {
			try {
				return DataFileUtil.read(path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).whenComplete((tag, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				report.accept(this.error(this.ctx.tr("import.failed", name, ErrorUtil.message(failure))));
				return;
			}
			if (tag == null) {
				report.accept(this.error(this.ctx.tr("file.not_compound", name)));
				return;
			}
			this.importTag(path, tag, options, report);
		}));
	}

	private void openFile(Path path, Consumer<ImportMessage> report) {
		String name = path.getFileName().toString();
		if (this.browser.open(path)) {
			report.accept(this.info(this.ctx.tr("import.opened", name)));
		} else {
			report.accept(this.error(this.ctx.tr("import.unsupported", name)));
		}
	}

	private void importTag(Path path, CompoundTag tag, ImportOptions options, Consumer<ImportMessage> report) {
		String name = path.getFileName().toString();
		DataFixService fixes = this.ctx.client().dataFixes();
		int current = fixes.dataVersion();
		boolean stated = tag.containsNumber(ExportUtil.DATA_VERSION);
		int version = options.dataVersion() > 0 ? options.dataVersion() : stated ? tag.getInt(ExportUtil.DATA_VERSION, current) : current;
		ExportData data = ExportUtil.fromFile(tag, version);
		if (data == null) {
			this.openFile(path, report);
			return;
		}
		if (options.dataVersion() > 0) {
			data = new ExportData(data.kind(), data.tag(), options.dataVersion());
		}
		if (data.dataVersion() > current) {
			report.accept(this.error(this.ctx.tr("import.newer", name, GameVersion.byDataVersion(data.dataVersion()).name())));
			return;
		}
		if (!stated && options.dataVersion() <= 0) {
			report.accept(this.info(this.ctx.tr("import.no_version", name)));
		}
		ExportData upgraded;
		try {
			upgraded = ExportUtil.upgrade(data, fixes);
		} catch (RuntimeException e) {
			report.accept(this.error(this.ctx.tr("import.failed", name, ErrorUtil.message(e))));
			return;
		}
		if (upgraded.kind() == SubjectKind.BLOCK) {
			this.place(name, upgraded.tag(), options.at(), report);
		} else if (upgraded.kind() == SubjectKind.ENTITY) {
			this.summon(name, upgraded.tag(), options.at(), report);
		} else {
			this.give(name, upgraded.tag(), report);
		}
	}

	private void give(String name, CompoundTag tag, Consumer<ImportMessage> report) {
		if (!this.ctx.client().player().creative()) {
			report.accept(this.error(this.ctx.tr("import.creative")));
			return;
		}
		GameItem item = this.ctx.client().items().fromTag(tag);
		if (!InventoryUtil.present(item)) {
			report.accept(this.error(this.ctx.tr("import.invalid", name)));
			return;
		}
		if (!this.ctx.client().player().give(item)) {
			report.accept(this.error(this.ctx.tr("import.inventory_full")));
			return;
		}
		report.accept(this.info(this.ctx.tr("import.given", item.name())));
	}

	private Optional<WorldBackend> backend(SubjectKind kind, Consumer<ImportMessage> report) {
		Optional<WorldBackend> backend = this.ctx.services().find(WorldBackends.class).flatMap(backends -> backends.best(kind, TargetCapability.WRITE));
		if (backend.isEmpty()) {
			report.accept(this.error(this.ctx.tr("import.no_backend")));
		}
		return backend;
	}

	private void place(String name, CompoundTag doc, Position at, Consumer<ImportMessage> report) {
		Optional<WorldBackend> backend = this.backend(SubjectKind.BLOCK, report);
		if (backend.isEmpty()) {
			return;
		}
		String blockId = doc.getString(BlockDocUtil.NAME, "");
		Text blockName = this.ctx.client().registries().name(RegistryService.BLOCK, blockId);
		backend.get().writeBlock(this.ctx.client().world().dimension(), at.block(), BlockStateUtil.sync(BlockDocUtil.fromTag(doc)), BlockWriteFlags.DEFAULT).whenComplete((ignored, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				report.accept(this.error(this.ctx.tr("import.failed", name, ErrorUtil.message(failure))));
			} else {
				report.accept(this.info(this.ctx.tr("import.placed", blockName, at.block().command())));
			}
		}));
	}

	private void summon(String name, CompoundTag tag, Position at, Consumer<ImportMessage> report) {
		Optional<WorldBackend> backend = this.backend(SubjectKind.ENTITY, report);
		if (backend.isEmpty()) {
			return;
		}
		CompoundTag entity = PickItemUtil.summonable(tag);
		ListTag pos = new ListTag();
		pos.add(DoubleTag.of(at.x()));
		pos.add(DoubleTag.of(at.y()));
		pos.add(DoubleTag.of(at.z()));
		entity.put("Pos", pos);
		Text entityName = this.ctx.client().registries().name(RegistryService.ENTITY_TYPE, entity.getString("id", ""));
		backend.get().summonEntity(this.ctx.client().world().dimension(), entity).whenComplete((ignored, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				report.accept(this.error(this.ctx.tr("import.failed", name, ErrorUtil.message(failure))));
			} else {
				report.accept(this.info(this.ctx.tr("import.summoned", entityName)));
			}
		}));
	}

	private void importImage(Path path, ImportOptions options, Consumer<ImportMessage> report) {
		String name = path.getFileName().toString();
		this.ctx.platform().scheduler().async(() -> {
			try {
				BufferedImage image = ImageLoreUtil.read(path);
				int[] size = ImageLoreUtil.size(image.getWidth(), image.getHeight(), options.imageWidth(), options.imageHeight());
				return ImageLoreUtil.lore(image, size[0], size[1]);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).whenComplete((lore, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				report.accept(this.error(this.ctx.tr("import.failed", name, ErrorUtil.message(failure))));
				return;
			}
			GameVersion version = this.ctx.platform().game().version();
			CompoundTag item = ItemStackUtil.create(PAINTING, 1, version);
			ItemFields.CUSTOM_NAME.write(item, version, Text.literal(ImageLoreUtil.baseName(path), Style.EMPTY.withColor(Formatting.GOLD).withItalic(false)));
			ItemFields.LORE.write(item, version, lore);
			this.give(name, item, report);
		}));
	}

	private ImportMessage info(Text text) {
		return new ImportMessage(text, false);
	}

	private ImportMessage error(Text text) {
		return new ImportMessage(text, true);
	}
}
