package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.PresetItem;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.CreditsUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SkullUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.List;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GetCommands {
	public static final String ALIAS = "g";

	private final ModuleContext ctx;

	public GetCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("item")
			.then(CommandNode.argument("item", ArgSpec.id().suggest(this::itemIds)).executes(context -> this.item(context, 1))
				.then(CommandNode.argument("count", ArgSpec.integer(1, Integer.MAX_VALUE)).executes(context -> this.item(context, context.integer("count"))))), "i");
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("snbt").then(CommandNode.argument("snbt", ArgSpec.greedy()).executes(this::snbt)));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("preset").then(CommandNode.argument("preset", ArgSpec.word().suggest(this::presets)).executes(this::preset)));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("skull").then(CommandNode.argument("player", ArgSpec.word()).executes(this::skull)), "sk");
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("credits").executes(this::credits));
	}

	private void credits(CommandContext context) {
		context.feedback(this.ctx.tr("credits.title").withStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
		context.feedback(this.ctx.tr("credits.original").withStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		context.feedback(this.ctx.tr("credits.heads").withStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		List<String> translators = CreditsUtil.translators();
		if (translators.isEmpty()) {
			return;
		}
		context.feedback(this.ctx.tr("credits.translations").withStyle(Style.EMPTY.withColor(Formatting.GOLD)));
		for (String line : translators) {
			context.feedback(Text.literal(line, Style.EMPTY.withColor(Formatting.GRAY)));
		}
	}

	private List<String> itemIds() {
		return this.ctx.client().registries().ids(RegistryService.ITEM);
	}

	private List<String> presets() {
		return this.ctx.registry(PresetItem.REGISTRY).ids();
	}

	private void item(CommandContext context, int count) throws CommandException {
		String id = context.string("item");
		if (!this.ctx.client().items().isItem(id)) {
			throw new CommandException(CommandUtil.error("invalid_item"));
		}
		GameVersion version = CommandUtil.version(this.ctx);
		int max = ItemStackUtil.maxCount(version);
		if (count > max) {
			throw new CommandException(this.ctx.tr("item.too_many", max));
		}
		this.give(context, ItemStackUtil.create(id, count, version));
	}

	private void snbt(CommandContext context) throws CommandException {
		CompoundTag tag;
		try {
			tag = SnbtUtil.parseCompound(context.string("snbt"), CommandUtil.dialect(CommandUtil.version(this.ctx)));
		} catch (SnbtException e) {
			throw new CommandException(CommandUtil.error("invalid_snbt", e.getMessage()));
		}
		if (!tag.contains("id")) {
			throw new CommandException(CommandUtil.error("invalid_item"));
		}
		if (ItemStackUtil.count(tag) <= 0) {
			ItemStackUtil.setCount(tag, 1, CommandUtil.version(this.ctx));
		}
		this.give(context, tag);
	}

	private void preset(CommandContext context) throws CommandException {
		Optional<PresetItem> preset = this.ctx.registry(PresetItem.REGISTRY).get(context.string("preset"));
		if (preset.isEmpty()) {
			throw new CommandException(this.ctx.tr("preset.unknown", context.string("preset")));
		}
		this.give(context, preset.get().create(CommandUtil.version(this.ctx)));
	}

	private void skull(CommandContext context) throws CommandException {
		String player = context.string("player");
		GameVersion version = CommandUtil.version(this.ctx);
		CompoundTag tag = ItemStackUtil.create(SkullUtil.PLAYER_HEAD, 1, version);
		if (Feature.ITEM_COMPONENTS.in(version)) {
			CompoundTag profile = new CompoundTag();
			profile.putString("name", player);
			SkullUtil.setProfile(tag, true, version, profile);
		} else {
			SkullUtil.setProfile(tag, true, version, StringTag.of(player));
		}
		this.give(context, tag);
	}

	private void give(CommandContext context, CompoundTag tag) throws CommandException {
		CommandUtil.give(this.ctx, context, tag, null);
		context.feedback(this.ctx.tr("given", this.ctx.client().items().fromTag(tag).name()));
	}
}
