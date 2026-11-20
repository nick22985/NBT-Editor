package com.nick22985.nbteditor.modules.enchantments;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.Field;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.EnchantmentInfo;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EnchantmentCommands {
	private final ModuleContext ctx;

	public EnchantmentCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		int cap = ItemDataUtil.maxLevel(CommandUtil.version(this.ctx));
		CommandNode node = CommandNode.literal("max").executes(context -> this.max(context, false, false))
			.then(CommandNode.literal("cursed").executes(context -> this.max(context, false, true))
				.then(this.all(cap, true))
				.then(this.level(cap, false, true)))
			.then(this.all(cap, false))
			.then(this.level(cap, false, false));
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, node, "m");
	}

	private CommandNode all(int cap, boolean cursed) {
		return CommandNode.literal("all").executes(context -> this.max(context, true, cursed)).then(this.level(cap, true, cursed));
	}

	private CommandNode level(int cap, boolean all, boolean cursed) {
		return CommandNode.argument("level", ArgSpec.integer(1, cap)).executes(context -> this.max(context, all, cursed));
	}

	private void max(CommandContext context, boolean all, boolean cursed) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		GameItem held = CommandUtil.heldItem(this.ctx);
		Integer level = context.has("level") ? context.integer("level") : null;
		RegistryService registries = this.ctx.client().registries();
		Map<String, Integer> chosen = new LinkedHashMap<>();
		for (String id : registries.ids(RegistryService.ENCHANTMENT)) {
			EnchantmentInfo info = registries.enchantment(id);
			if (info == null || (info.curse() && !cursed) || (!all && !registries.canEnchant(id, held))) {
				continue;
			}
			chosen.put(id, level == null ? info.maxLevel() : level);
		}
		if (chosen.isEmpty()) {
			throw new CommandException(this.ctx.tr("command.max.none"));
		}
		Field<Map<String, Integer>> field = EnchantmentsPane.BOOK.equals(held.id()) ? ItemFields.STORED_ENCHANTMENTS : ItemFields.ENCHANTMENTS;
		CommandUtil.editHeld(this.ctx, context, tag -> {
			Map<String, Integer> current = field.get(tag, version);
			Map<String, Integer> next = current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
			next.putAll(chosen);
			field.write(tag, version, next);
		}, this.ctx.tr("command.max.done", chosen.size()));
	}
}
