package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandExecutor;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.command.CommandService;
import com.nick22985.nbteditor.command.HelpTopic;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.CommandUtil;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandTree {
	private static final String HELP = "help";

	private final ModuleContext ctx;
	private final CommandsSettings settings;
	private final HelpPrinter help;
	private final Map<String, CommandNode> roots = new LinkedHashMap<>();

	public CommandTree(ModuleContext ctx, CommandsSettings settings, HelpPrinter help) {
		this.ctx = ctx;
		this.settings = settings;
		this.help = help;
	}

	public void build() {
		Map<String, CommandNode> groups = new LinkedHashMap<>();
		groups.put(CommandContribution.NBT, this.group(CommandContribution.NBT, context -> CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), null)));
		groups.put(CommandContribution.GET, this.group(CommandContribution.GET, context -> this.help.print(context, CommandContribution.GET)));
		groups.put(CommandContribution.FACTORY, this.group(CommandContribution.FACTORY, context -> CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), FactoryCommands.DEFAULT_TAB)));
		boolean extreme = this.settings.extremeAliases();
		for (CommandContribution contribution : this.ctx.registry(CommandContribution.REGISTRY).values()) {
			if (contribution.root()) {
				this.roots.put(contribution.node().name(), contribution.node());
				continue;
			}
			CommandNode group = groups.get(contribution.group());
			if (group == null) {
				this.ctx.log().warn("Unknown command group " + contribution.group() + " for " + contribution.id());
				continue;
			}
			if (group.child(contribution.node().name()) == null) {
				group.then(contribution.node());
			}
		}
		for (Map.Entry<String, CommandNode> entry : groups.entrySet()) {
			CommandNode group = entry.getValue();
			String name = entry.getKey();
			CommandNode help = CommandNode.literal(HELP).executes(context -> this.help.print(context, name));
			for (HelpTopic topic : this.ctx.registry(HelpTopic.REGISTRY).values()) {
				if (name.equals(topic.group()) && help.child(topic.name()) == null) {
					help.then(CommandNode.literal(topic.name()).executes(context -> this.help.topic(context, topic)));
				}
			}
			group.then(help);
			if (extreme) {
				for (CommandContribution contribution : this.ctx.registry(CommandContribution.REGISTRY).values()) {
					String alias = contribution.alias();
					if (name.equals(contribution.group()) && alias != null && group.child(alias) == null) {
						group.then(contribution.node().renamed(alias));
					}
				}
			}
			this.roots.put(name, group);
		}
		Map<String, CommandNode> extra = new LinkedHashMap<>();
		if (extreme) {
			extra.put(NbtCommands.ALIAS, groups.get(CommandContribution.NBT).renamed(NbtCommands.ALIAS));
			extra.put(GetCommands.ALIAS, groups.get(CommandContribution.GET).renamed(GetCommands.ALIAS));
			extra.put(FactoryCommands.ALIAS, groups.get(CommandContribution.FACTORY).renamed(FactoryCommands.ALIAS));
			for (CommandContribution contribution : this.ctx.registry(CommandContribution.REGISTRY).values()) {
				if (contribution.root() && contribution.alias() != null) {
					extra.put(contribution.alias(), contribution.node().renamed(contribution.alias()));
				}
			}
		}
		for (Map.Entry<String, String> alias : this.settings.aliases().entrySet()) {
			String name = alias.getKey().trim().toLowerCase(Locale.ROOT);
			CommandNode target = this.resolve(alias.getValue());
			if (name.isEmpty() || name.contains(" ")) {
				continue;
			}
			if (target == null) {
				this.ctx.log().warn("Alias /" + name + " points at unknown command /" + alias.getValue());
				continue;
			}
			extra.put(name, target.renamed(name));
		}
		CommandService commands = this.ctx.client().commands();
		for (CommandNode root : this.roots.values()) {
			commands.register(root);
		}
		for (Map.Entry<String, CommandNode> entry : extra.entrySet()) {
			if (!this.roots.containsKey(entry.getKey())) {
				commands.register(entry.getValue());
			}
		}
	}

	private CommandNode group(String name, CommandExecutor executor) {
		return CommandNode.literal(name).executes(executor);
	}

	private CommandNode resolve(String path) {
		String[] parts = path.trim().replaceFirst("^/", "").split("\\s+");
		CommandNode node = this.roots.get(parts[0]);
		for (int i = 1; i < parts.length && node != null; i++) {
			node = node.child(parts[i]);
		}
		return node;
	}
}
