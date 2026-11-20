package com.nick22985.nbteditor.modules.gradient;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.TextMarkup;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.util.CommandUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GradientModule implements Module {
	@Override
	public String id() {
		return "text-gradient";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.register(TextMarkup.REGISTRY, "gradient", new GradientMarkup());
		ctx.register(TextMarkup.REGISTRY, "rainbow", new RainbowMarkup());
		ctx.register(RichTextAction.REGISTRY, "gradient", new GradientAction(ctx), 5);
		GradientHelp help = new GradientHelp(ctx);
		CommandUtil.helpTopic(ctx, CommandContribution.GET, "gradient", help::lines);
	}
}
