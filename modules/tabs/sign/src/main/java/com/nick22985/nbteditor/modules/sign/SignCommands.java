package com.nick22985.nbteditor.modules.sign;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SignUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SignCommands {
	private final ModuleContext ctx;

	public SignCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		ArgSpec sign = ArgSpec.id().suggest(this::signs);
		CommandNode node = CommandNode.literal("sign").executes(context -> CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), SignTab.ID))
			.then(CommandNode.literal("new").then(CommandNode.argument("sign", sign).executes(this::create)));
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, node, "s");
	}

	private List<String> signs() {
		List<String> signs = new ArrayList<>();
		for (String id : this.ctx.client().registries().ids(RegistryService.ITEM)) {
			if (SignUtil.isSign(id)) {
				signs.add(id);
			}
		}
		return signs;
	}

	private void create(CommandContext context) throws CommandException {
		String id = ItemStackUtil.namespaced(context.string("sign"));
		if (!SignUtil.isSign(id)) {
			throw new CommandException(this.ctx.tr("command.not_sign", id));
		}
		CommandUtil.openEditor(this.ctx, CommandUtil.newHeld(this.ctx, id), SignTab.ID);
	}
}
