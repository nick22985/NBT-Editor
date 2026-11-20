package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.HelpTopic;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.ClickAction;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.HoverEvent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.CommandUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HelpPrinter {
	private final ModuleContext ctx;

	public HelpPrinter(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void print(CommandContext context, String group) {
		boolean all = CommandContribution.NBT.equals(group);
		context.feedback(this.ctx.tr(all ? "help.title" : "help.group", "/" + group).withStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
		for (CommandContribution contribution : this.ctx.registry(CommandContribution.REGISTRY).values()) {
			if (contribution.group().equals(group) || all && contribution.root()) {
				context.feedback(this.line(contribution));
			}
		}
		for (HelpTopic topic : this.ctx.registry(HelpTopic.REGISTRY).values()) {
			if (topic.group().equals(group)) {
				context.feedback(this.topicLine(topic));
			}
		}
		if (all) {
			context.feedback(this.ctx.tr("help.more").withStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		}
	}

	public void topic(CommandContext context, HelpTopic topic) {
		for (Text line : topic.lines().get()) {
			context.feedback(line);
		}
	}

	private Text topicLine(HelpTopic topic) {
		String command = "/" + topic.group() + " help " + topic.name();
		Style style = Style.EMPTY.withColor(Formatting.YELLOW)
			.withClickEvent(new ClickEvent(ClickAction.RUN_COMMAND, command))
			.withHoverEvent(HoverEvent.showText(this.ctx.tr("help.run")));
		return Text.join(List.of(Text.literal(command, style), Text.literal(" - ", Style.EMPTY.withColor(Formatting.DARK_GRAY)), topic.description().withStyle(Style.EMPTY.withColor(Formatting.GRAY))));
	}

	private Text line(CommandContribution contribution) {
		String usage = CommandUtil.usage(contribution.node());
		String command = "/" + (contribution.root() ? "" : contribution.group() + " ") + usage;
		String suggestion = "/" + contribution.id() + " ";
		Style style = Style.EMPTY.withColor(Formatting.YELLOW)
			.withClickEvent(new ClickEvent(ClickAction.SUGGEST_COMMAND, suggestion))
			.withHoverEvent(HoverEvent.showText(this.ctx.tr("help.click")));
		List<Text> parts = new ArrayList<>();
		parts.add(Text.literal(command, style));
		if (contribution.alias() != null) {
			parts.add(Text.literal(" (" + contribution.alias() + ")", Style.EMPTY.withColor(Formatting.DARK_GRAY)));
		}
		parts.add(Text.literal(" - ", Style.EMPTY.withColor(Formatting.DARK_GRAY)));
		parts.add(contribution.description().withStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		return Text.join(parts);
	}
}
