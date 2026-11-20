package com.nick22985.nbteditor.modules.book;

import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.BookContent;
import com.nick22985.nbteditor.data.BookField;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.util.BookUtil;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookCommands {
	private final ModuleContext ctx;

	public BookCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandNode node = CommandNode.literal(BookTab.ID).executes(context -> CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), BookTab.ID))
			.then(CommandNode.literal("new").executes(this::create))
			.then(CommandNode.literal("make_writable").executes(this::makeWritable));
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, node, "b");
	}

	private void create(CommandContext context) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		EditorService editor = CommandUtil.service(this.ctx, EditorService.class);
		Target target = CommandUtil.newHeld(this.ctx, BookField.WRITTEN);
		CompoundTag tag = ItemStackUtil.create(BookField.WRITTEN, 1, version);
		ItemFields.BOOK.write(tag, version, BookContent.empty(true));
		target.save(tag).whenComplete((ignored, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
			} else {
				editor.open(target, BookTab.ID);
			}
		}));
	}

	private void makeWritable(CommandContext context) throws CommandException {
		if (!BookUtil.written(CommandUtil.heldItem(this.ctx).tag())) {
			throw new CommandException(this.ctx.tr("command.make_writable.not_written"));
		}
		GameVersion version = CommandUtil.version(this.ctx);
		CommandUtil.editHeld(this.ctx, context, tag -> BookUtil.convert(tag, version), this.ctx.tr("command.make_writable.done"));
	}
}
