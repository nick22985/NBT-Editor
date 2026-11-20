package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.util.CommandUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerCommands {
	private final ModuleContext ctx;
	private final ContainerOpener opener;

	public ContainerCommands(ModuleContext ctx, ContainerOpener opener) {
		this.ctx = ctx;
		this.opener = opener;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.ROOT, CommandNode.literal("open").executes(context -> this.open(CommandUtil.heldOrTargeted(this.ctx))).then(CommandNode.literal("echest").executes(context -> CommandUtil.later(this.ctx, this.opener::openEnderChest))), "o");
	}

	private void open(Target target) throws CommandException {
		CommandUtil.later(this.ctx, () -> this.opener.open(target));
	}
}
