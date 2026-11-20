package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.util.CommandUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemCommands {
	private final ModuleContext ctx;
	private final ItemTargets targets;

	public ItemCommands(ModuleContext ctx, ItemTargets targets) {
		this.ctx = ctx;
		this.targets = targets;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("slot").then(CommandNode.argument("slot", ArgSpec.integer(0, PlayerService.OFFHAND_SLOT)).executes(context -> CommandUtil.openEditor(this.ctx, this.targets.slot(context.integer("slot")), null))), "sl");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("offhand").executes(context -> CommandUtil.openEditor(this.ctx, this.targets.held(Hand.OFF), null)), "oh");
	}
}
