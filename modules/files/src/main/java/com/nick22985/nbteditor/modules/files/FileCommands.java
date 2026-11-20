package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.CommandUtil;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FileCommands {
	private final ModuleContext ctx;
	private final FileBrowser browser;
	private final Importer importer;
	private final Exporter exporter;

	public FileCommands(ModuleContext ctx, FileBrowser browser, Importer importer, Exporter exporter) {
		this.ctx = ctx;
		this.browser = browser;
		this.importer = importer;
		this.exporter = exporter;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("file").executes(context -> CommandUtil.later(this.ctx, () -> this.browser.browse(this.browser.startDir())))
			.then(CommandNode.argument("path", ArgSpec.greedy()).executes(this::openPath)), "f");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("import").executes(context -> CommandUtil.later(this.ctx, () -> this.ctx.client().screens().open(new ImportScreen(this.ctx, this.browser, this.importer)))), "i");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("export").executes(this.exporter::file)
			.then(CommandNode.literal("cmd").executes(this.exporter::command))
			.then(CommandNode.literal("cmdblock").executes(this.exporter::commandBlock))
			.then(CommandNode.literal("get").executes(this.exporter::getCommand))
			.then(CommandNode.literal("item").executes(this.exporter::item))
			.then(CommandNode.literal("file").executes(this.exporter::file).then(CommandNode.argument("name", ArgSpec.greedy()).executes(this.exporter::file)))
			.then(CommandNode.literal("json").executes(context -> this.exporter.json(context, false))
				.then(CommandNode.literal("file").executes(context -> this.exporter.json(context, true)).then(CommandNode.argument("name", ArgSpec.greedy()).executes(context -> this.exporter.json(context, true))))), "x");
	}

	private void openPath(CommandContext context) throws CommandException {
		Path path = this.browser.resolve(context.string("path"));
		if (!Files.exists(path)) {
			throw new CommandException(this.ctx.tr("command.missing", path.toString()));
		}
		if (!Files.isDirectory(path) && !this.importer.importable(path)) {
			throw new CommandException(this.ctx.tr("browser.unsupported", path.getFileName().toString()));
		}
		CommandUtil.later(this.ctx, () -> {
			if (!this.browser.open(path)) {
				context.error(this.ctx.tr("browser.unsupported", path.getFileName().toString()));
			}
		});
	}
}
