package com.nick22985.nbteditor.modules.attributes;

import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.AttributeModifier;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.CommandUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class AttributeCommands {
	private final ModuleContext ctx;

	public AttributeCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandNode node = CommandNode.literal(AttributesTab.ID).executes(context -> CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), AttributesTab.ID))
			.then(CommandNode.literal("newuuids").executes(this::newIds));
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, node, "a");
	}

	private void newIds(CommandContext context) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		List<AttributeModifier> modifiers = ItemFields.ATTRIBUTE_MODIFIERS.get(CommandUtil.heldItem(this.ctx).tag(), version);
		if (modifiers == null || modifiers.isEmpty()) {
			throw new CommandException(this.ctx.tr("command.newuuids.none"));
		}
		List<AttributeModifier> renewed = new ArrayList<>();
		for (AttributeModifier modifier : modifiers) {
			renewed.add(modifier.withId(AttributeModifier.create(modifier.attribute(), modifier.amount(), modifier.operation(), modifier.slot(), version).id()));
		}
		CommandUtil.editHeld(this.ctx, context, tag -> ItemFields.ATTRIBUTE_MODIFIERS.write(tag, version, renewed), this.ctx.tr("command.newuuids.done", renewed.size()));
	}
}
