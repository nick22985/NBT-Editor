package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.TextMarkup;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.util.CommandUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FancyModule implements Module {
	@Override
	public String id() {
		return "text-fancy";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		TextSyntax syntax = ctx.services().provide(TextSyntax.class, new FancyTextSyntax(new FancyGrammar(ctx.registry(TextMarkup.REGISTRY))));
		ctx.register(RichTextAction.REGISTRY, "events", new EventsAction(ctx, syntax), 10);
		ctx.register(RichTextAction.REGISTRY, "insert", new InsertAction(ctx, syntax), 20);
		ctx.register(RichTextAction.REGISTRY, "font", new FontAction(ctx), 30);
		TextFormatHelp help = new TextFormatHelp(ctx);
		CommandUtil.helpTopic(ctx, CommandContribution.GET, "textformat", help::lines);
	}
}
