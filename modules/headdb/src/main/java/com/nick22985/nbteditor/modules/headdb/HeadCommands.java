package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.TextResolveUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadCommands {
	private static final String TAG_PREFIX = "tag:";
	private static final String BOX = "minecraft:shulker_box";
	private static final int BOX_SIZE = 27;
	private static final int MAX_HEADS = BOX_SIZE * BOX_SIZE;

	private final ModuleContext ctx;
	private final HeadDatabase database;
	private final HeadItems items;

	public HeadCommands(ModuleContext ctx, HeadDatabase database) {
		this.ctx = ctx;
		this.database = database;
		this.items = new HeadItems(ctx);
	}

	public void register() {
		List<String> categories = new ArrayList<>();
		for (HeadCategory category : HeadCategory.values()) {
			categories.add(category.id());
		}
		CommandNode node = CommandNode.literal("hdb").executes(context -> this.open(null))
			.then(CommandNode.literal("search").then(CommandNode.argument("query", ArgSpec.greedy()).executes(context -> this.open(context.string("query")))))
			.then(CommandNode.literal("tagsearch").then(CommandNode.argument("query", ArgSpec.greedy()).executes(context -> this.open(TAG_PREFIX + context.string("query")))))
			.then(CommandNode.literal("all")
				.then(CommandNode.argument("category", ArgSpec.choice(categories))
					.executes(context -> this.all(context, null))
					.then(CommandNode.literal("search").then(CommandNode.argument("query", ArgSpec.greedy()).executes(context -> this.all(context, context.string("query")))))))
			.then(CommandNode.literal("update").executes(this::update))
			.then(CommandNode.argument("query", ArgSpec.greedy()).executes(context -> this.open(context.string("query"))));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, node, "headdb");
	}

	private void open(String query) {
		CommandUtil.later(this.ctx, () -> this.ctx.client().screens().open(new HeadDbScreen(this.ctx, this.database, query)));
	}

	private void update(CommandContext context) {
		context.feedback(this.ctx.tr("command.update.started"));
		this.database.load(true).whenComplete((state, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null || state == HeadDbState.FAILED) {
				context.error(this.ctx.tr("status.failed"));
			} else {
				context.feedback(this.ctx.tr(state == HeadDbState.OFFLINE ? "command.update.offline" : "command.update.done", this.database.count(HeadCategory.ALL)));
			}
		}));
	}

	private void all(CommandContext context, String query) throws CommandException {
		HeadCategory category = this.category(context.string("category"));
		ContainerService containers = CommandUtil.service(this.ctx, ContainerService.class);
		if (!this.ctx.client().player().creative()) {
			throw new CommandException(CommandUtil.error("creative"));
		}
		this.database.load(false).whenComplete((state, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
				return;
			}
			List<Head> heads = this.database.search(category, query);
			if (heads.isEmpty()) {
				context.error(this.ctx.tr("command.all.none"));
				return;
			}
			try {
				this.give(context, containers, category, query, heads);
			} catch (CommandException e) {
				context.error(e.text());
			}
		}));
	}

	private void give(CommandContext context, ContainerService containers, HeadCategory category, String query, List<Head> heads) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		List<Head> given = heads.subList(0, Math.min(heads.size(), MAX_HEADS));
		List<CompoundTag> tags = new ArrayList<>();
		for (Head head : given) {
			tags.add(this.items.create(head).tag());
		}
		Text title = this.ctx.tr(category.langKey());
		if (query != null) {
			title = this.ctx.tr("command.all.search", title, query);
		}
		String plain = TextResolveUtil.resolve(title, this.ctx.client().screens().host());
		Style style = Style.EMPTY.withItalic(false).withBold(true).withColor(category.color());
		CompoundTag box;
		if (tags.size() <= BOX_SIZE) {
			box = this.box(containers, version, Text.literal(plain).withStyle(style), tags);
		} else {
			List<CompoundTag> boxes = new ArrayList<>();
			for (int start = 0; start < tags.size(); start += BOX_SIZE) {
				int page = start / BOX_SIZE + 1;
				boxes.add(this.box(containers, version, Text.literal(plain + " " + page).withStyle(style), tags.subList(start, Math.min(tags.size(), start + BOX_SIZE))));
			}
			box = this.box(containers, version, Text.literal(plain).withStyle(style), boxes);
		}
		Text done = given.size() < heads.size() ? this.ctx.tr("command.all.capped", given.size(), heads.size()) : this.ctx.tr("command.all.done", given.size());
		CommandUtil.give(this.ctx, context, box, done);
	}

	private CompoundTag box(ContainerService containers, GameVersion version, Text name, List<CompoundTag> contents) throws CommandException {
		CompoundTag box = ItemStackUtil.create(BOX, 1, version);
		ItemFields.CUSTOM_NAME.write(box, version, name);
		ContainerSubject subject = containers.subject(SubjectKind.ITEM, box, version);
		Optional<ContainerFormat> format = containers.format(subject);
		if (format.isEmpty()) {
			throw new CommandException(CommandUtil.error("invalid_item"));
		}
		List<CompoundTag> slots = new ArrayList<>(format.get().read(subject));
		for (int slot = 0; slot < contents.size() && slot < slots.size(); slot++) {
			slots.set(slot, contents.get(slot));
		}
		format.get().write(subject, slots);
		return box;
	}

	private HeadCategory category(String id) throws CommandException {
		String wanted = id.toLowerCase(Locale.ROOT);
		for (HeadCategory category : HeadCategory.values()) {
			if (category.id().equals(wanted)) {
				return category;
			}
		}
		throw new CommandException(this.ctx.tr("command.all.unknown", id));
	}
}
