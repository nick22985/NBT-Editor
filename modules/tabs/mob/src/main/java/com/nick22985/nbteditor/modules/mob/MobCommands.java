package com.nick22985.nbteditor.modules.mob;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.util.CommandUtil;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MobCommands {
	private final ModuleContext ctx;

	public MobCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandNode node = CommandNode.literal("mob").executes(context -> CommandUtil.openEditor(this.ctx, this.target(), MobTab.ID));
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, node, "mo");
	}

	private Target target() throws CommandException {
		Optional<Target> targeted = CommandUtil.targeted(this.ctx);
		if (targeted.isPresent() && targeted.get().kind() == SubjectKind.ENTITY) {
			return targeted.get();
		}
		return CommandUtil.heldOrTargeted(this.ctx);
	}
}
