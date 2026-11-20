package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandsModule implements Module {
	@Override
	public String id() {
		return "commands";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.client().chat().longInput(ctx.config().bool("long_chat", true)::get);
		new NbtCommands(ctx).register();
		new GetCommands(ctx).register();
	}

	@Override
	public void ready(ModuleContext ctx) {
		new FactoryCommands(ctx).register();
		new CommandTree(ctx, new CommandsSettings(ctx), new HelpPrinter(ctx)).build();
	}
}
