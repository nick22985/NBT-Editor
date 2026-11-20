package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.ui.settings.SettingsScreen;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtCommands {
	public static final String ALIAS = "n";

	private final ModuleContext ctx;

	public NbtCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("config").executes(this::config), "c");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("new").then(CommandNode.argument("item", ArgSpec.id().suggest(this::itemIds)).executes(this::create)), "n");
	}

	private List<String> itemIds() {
		return this.ctx.client().registries().ids(RegistryService.ITEM);
	}

	private void config(CommandContext context) {
		CommandUtil.later(this.ctx, () -> this.ctx.client().screens().open(new SettingsScreen(this.ctx.kernel())));
	}

	private void create(CommandContext context) throws CommandException {
		CommandUtil.openEditor(this.ctx, CommandUtil.newHeld(this.ctx, context.string("item")), null);
	}
}
