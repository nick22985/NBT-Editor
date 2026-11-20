package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.module.Registry;
import com.nick22985.nbteditor.util.CommandUtil;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FactoryCommands {
	public static final String ALIAS = "f";
	public static final String DEFAULT_TAB = "display";
	private static final String NBT_TAB = "nbt";
	private static final Map<String, String> ALIASES = Map.ofEntries(
		Map.entry("display", "d"),
		Map.entry("enchantments", "e"),
		Map.entry("attributes", "a"),
		Map.entry("tooltip", "t"),
		Map.entry("blockstates", "bs"),
		Map.entry("book", "b"),
		Map.entry("sign", "s"),
		Map.entry("potion", "p"),
		Map.entry("armor", "ar"),
		Map.entry("misc", "mi"),
		Map.entry("container", "c"));

	private final ModuleContext ctx;

	public FactoryCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		Registry<CommandContribution> commands = this.ctx.registry(CommandContribution.REGISTRY);
		for (EditorTab tab : this.ctx.registry(EditorTab.REGISTRY).values()) {
			if (NBT_TAB.equals(tab.id())) {
				continue;
			}
			String name = tab.id().replace("_", "");
			if (commands.contains(CommandContribution.FACTORY + " " + name)) {
				continue;
			}
			String id = tab.id();
			CommandNode node = CommandNode.literal(name).executes(context -> CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), id));
			CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, node, ALIASES.get(name), this.ctx.tr("command.tab", tab.label()));
		}
	}
}
