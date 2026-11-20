package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.CommandUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItemCommands {
	private final ModuleContext ctx;
	private final LostItems lost;

	public LostItemCommands(ModuleContext ctx, LostItems lost) {
		this.ctx = ctx;
		this.lost = lost;
	}

	public void register() {
		List<String> sources = new ArrayList<>();
		for (LostItemSource source : LostItemSource.values()) {
			sources.add(source.serializedName());
		}
		CommandNode node = CommandNode.literal("lostitem").executes(context -> this.open(LostItemSource.CURSOR))
			.then(CommandNode.literal("latest").executes(this::latest))
			.then(CommandNode.argument("source", ArgSpec.choice(sources)).executes(this::source));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, node, "li");
	}

	private void open(LostItemSource source) {
		CommandUtil.later(this.ctx, () -> this.lost.open(source));
	}

	private void source(CommandContext context) throws CommandException {
		LostItemSource source = LostItemSource.byName(context.string("source"));
		if (source == null) {
			throw new CommandException(this.ctx.tr("command.unknown_source", context.string("source")));
		}
		this.open(source);
	}

	private void latest(CommandContext context) throws CommandException {
		if (this.lost.history().latest() == null) {
			throw new CommandException(this.ctx.tr("command.none"));
		}
		if (!this.ctx.client().player().creative()) {
			throw new CommandException(CommandUtil.error("creative"));
		}
		if (!this.lost.giveLatest()) {
			throw new CommandException(CommandUtil.error("inventory_full"));
		}
		context.feedback(this.ctx.tr("command.given"));
	}
}
