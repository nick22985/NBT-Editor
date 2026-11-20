package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.CommandUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelCommands {
	private final ModuleContext ctx;

	public ModelCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandNode node = CommandNode.literal("models").executes(context -> this.open(null))
			.then(CommandNode.argument("query", ArgSpec.greedy()).executes(context -> this.open(context.string("query"))));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, node, "itemmodels");
	}

	private void open(String query) {
		CommandUtil.later(this.ctx, () -> this.ctx.client().screens().open(new PackModelsScreen(this.ctx, query)));
	}
}
