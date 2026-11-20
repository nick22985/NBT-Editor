package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.data.ExportData;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.text.ClickAction;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockDocUtil;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ExportUtil;
import com.nick22985.nbteditor.util.NbtJsonUtil;
import com.nick22985.nbteditor.util.PickItemUtil;
import com.nick22985.nbteditor.util.TextResolveUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Exporter {
	public static final String NBT = "nbt";
	public static final String JSON = "json";

	private final ModuleContext ctx;
	private final FileBrowser browser;

	public Exporter(ModuleContext ctx, FileBrowser browser) {
		this.ctx = ctx;
		this.browser = browser;
	}

	private GameVersion version() {
		return CommandUtil.version(this.ctx);
	}

	private SnbtDialect dialect() {
		return CommandUtil.dialect(this.version());
	}

	public void withData(CommandContext context, BiConsumer<Target, ExportData> action) throws CommandException {
		Target target = CommandUtil.heldOrTargeted(this.ctx);
		SubjectKind kind = target.kind() == SubjectKind.PLAYER ? SubjectKind.ENTITY : target.kind();
		if (kind != SubjectKind.ITEM && kind != SubjectKind.BLOCK && kind != SubjectKind.ENTITY) {
			throw new CommandException(this.ctx.tr("export.unsupported", target.displayName()));
		}
		int dataVersion = this.ctx.client().dataFixes().dataVersion();
		target.load().whenComplete((tag, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
				return;
			}
			try {
				action.accept(target, new ExportData(kind, tag, dataVersion));
			} catch (RuntimeException e) {
				context.error(ErrorUtil.message(e));
			}
		}));
	}

	public void command(CommandContext context) throws CommandException {
		this.withData(context, (target, data) -> {
			this.ctx.client().system().setClipboard("/" + ExportUtil.command(data, this.version(), this.dialect()));
			context.feedback(this.ctx.tr("export.copied_command", target.displayName()));
		});
	}

	public void commandBlock(CommandContext context) throws CommandException {
		this.withData(context, (target, data) -> {
			String command = ExportUtil.command(data, this.version(), this.dialect());
			this.give(context, ExportUtil.commandBlock(command, this.version(), this.dialect()), this.ctx.tr("export.command_block", target.displayName()));
		});
	}

	public void getCommand(CommandContext context) throws CommandException {
		this.withData(context, (target, data) -> {
			if (data.kind() != SubjectKind.ITEM) {
				context.error(this.ctx.tr("export.items_only"));
				return;
			}
			this.ctx.client().system().setClipboard("/" + ExportUtil.getCommand(data.tag(), this.dialect()));
			context.feedback(this.ctx.tr("export.copied_command", target.displayName()));
		});
	}

	public void item(CommandContext context) throws CommandException {
		this.withData(context, (target, data) -> {
			CompoundTag item = this.toItem(data);
			if (item == null) {
				context.error(this.ctx.tr("export.no_item", target.displayName()));
				return;
			}
			this.give(context, item, this.ctx.tr("export.given_item", target.displayName()));
		});
	}

	private CompoundTag toItem(ExportData data) {
		RegistryService registries = this.ctx.client().registries();
		if (data.kind() == SubjectKind.ITEM) {
			return data.tag().copy();
		}
		if (data.kind() == SubjectKind.BLOCK) {
			BlockData block = BlockDocUtil.fromTag(data.tag());
			String item = registries.blockItem(block.blockId());
			return item == null ? null : PickItemUtil.block(block, item, registries.defaultBlockState(block.blockId()), this.version());
		}
		String type = data.tag().getString(ExportUtil.ID, "minecraft:player");
		String item = PickItemUtil.entityItem(type, data.tag(), this.ctx.client().items()::isItem);
		return item == null ? null : PickItemUtil.entity(type, data.tag(), item, this.version());
	}

	private void give(CommandContext context, CompoundTag tag, Text done) {
		try {
			CommandUtil.give(this.ctx, context, tag, done);
		} catch (CommandException e) {
			context.error(e.text());
		}
	}

	public void file(CommandContext context) throws CommandException {
		this.withData(context, (target, data) -> {
			CompoundTag file = ExportUtil.toFile(data);
			this.write(context, this.name(context, target, NBT), path -> DataFileUtil.write(path, file));
		});
	}

	public void json(CommandContext context, boolean toFile) throws CommandException {
		this.withData(context, (target, data) -> {
			String json = NbtJsonUtil.pretty(data.tag());
			if (!toFile) {
				this.ctx.client().system().setClipboard(json);
				context.feedback(this.ctx.tr("export.copied_json", target.displayName()));
				return;
			}
			byte[] bytes = (json + "\n").getBytes(StandardCharsets.UTF_8);
			this.write(context, this.name(context, target, JSON), path -> DataFileUtil.writeAtomic(path, bytes));
		});
	}

	private String name(CommandContext context, Target target, String extension) {
		String base = context.has("name") ? context.string("name") : TextResolveUtil.resolve(target.displayName(), this.ctx.client().screens().host());
		return ExportUtil.withExtension(ExportUtil.fileName(base, LocalDateTime.now()), extension);
	}

	private void write(CommandContext context, String name, ExportWriter writer) {
		this.ctx.platform().scheduler().async(() -> {
			try {
				Path path = this.browser.ensureExportsDir().resolve(name);
				writer.write(path);
				return path;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).whenComplete((path, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(this.ctx.tr("file.write_failed", name, ErrorUtil.message(failure)));
				return;
			}
			Text link = Text.literal(path.getFileName().toString(), Style.EMPTY.withColor(Formatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent(ClickAction.OPEN_FILE, path.toString())));
			context.feedback(this.ctx.tr("export.saved", link));
		}));
	}
}
