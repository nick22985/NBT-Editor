package com.nick22985.nbteditor.modules.misc;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SignatureUtil;
import com.nick22985.nbteditor.util.SkullUtil;
import com.nick22985.nbteditor.util.UuidUtil;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MiscCommands {
	private final ModuleContext ctx;
	private final Signature signature;

	public MiscCommands(ModuleContext ctx, Signature signature) {
		this.ctx = ctx;
		this.signature = signature;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("randomuuid").executes(this::randomUuid)
			.then(CommandNode.literal("add").executes(this::randomUuid))
			.then(CommandNode.literal("remove").executes(this::removeUuid)), "ru");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("signature").executes(this::toggleSignature)
			.then(CommandNode.literal("set").then(CommandNode.argument("text", ArgSpec.greedy()).executes(this::setSignature)))
			.then(CommandNode.literal("reset").executes(this::resetSignature)), "sig");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("unbindskull").executes(this::unbind), "us");
	}

	private void randomUuid(CommandContext context) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		UUID uuid = UUID.randomUUID();
		CommandUtil.editHeld(this.ctx, context, tag -> ItemDataUtil.putCustomData(tag, version, MiscPane.UUID_KEY, UuidUtil.toIntArray(uuid)), this.ctx.tr("command.randomuuid.done", uuid.toString()));
	}

	private void removeUuid(CommandContext context) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		if (ItemDataUtil.customData(CommandUtil.heldItem(this.ctx).tag(), version, MiscPane.UUID_KEY) == null) {
			throw new CommandException(this.ctx.tr("command.randomuuid.none"));
		}
		CommandUtil.editHeld(this.ctx, context, tag -> ItemDataUtil.putCustomData(tag, version, MiscPane.UUID_KEY, null), this.ctx.tr("command.randomuuid.removed"));
	}

	private void toggleSignature(CommandContext context) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		boolean signed = SignatureUtil.has(CommandUtil.heldItem(this.ctx).tag(), version, this.signature.get());
		CommandUtil.editHeld(this.ctx, context, tag -> {
			if (signed) {
				SignatureUtil.remove(tag, version, this.signature.get());
			} else {
				SignatureUtil.add(tag, version, this.signature.get());
			}
		}, this.ctx.tr(signed ? "command.signature.removed" : "command.signature.added"));
	}

	private void setSignature(CommandContext context) {
		TextSyntax syntax = this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
		this.signature.set(syntax.parse(context.string("text")));
		context.feedback(this.ctx.tr("command.signature.set"));
	}

	private void resetSignature(CommandContext context) {
		this.signature.set(null);
		context.feedback(this.ctx.tr("command.signature.reset"));
	}

	private void unbind(CommandContext context) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		if (!SkullUtil.isPlayerHead(ItemStackUtil.id(CommandUtil.heldItem(this.ctx).tag()))) {
			throw new CommandException(this.ctx.tr("command.unbindskull.not_head"));
		}
		CommandUtil.editHeld(this.ctx, context, tag -> {
			if (!SkullUtil.unbind(tag, true, version)) {
				throw new CommandException(this.ctx.tr("unbind.no_texture"));
			}
		}, this.ctx.tr("command.unbindskull.done"));
	}
}
