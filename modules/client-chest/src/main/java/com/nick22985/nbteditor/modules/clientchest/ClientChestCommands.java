package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.ItemLostEvent;
import com.nick22985.nbteditor.hook.LostItemReason;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestCommands {
	private static final String TOP = "top";
	private static final String BOTTOM = "bottom";

	private final ModuleContext ctx;
	private final ClientChest chest;

	public ClientChestCommands(ModuleContext ctx, ClientChest chest) {
		this.ctx = ctx;
		this.chest = chest;
	}

	public void register() {
		CommandNode node = CommandNode.literal("clientchest").executes(context -> CommandUtil.later(this.ctx, this.chest::open))
			.then(this.transfer("import", true))
			.then(this.transfer("export", false))
			.then(CommandNode.argument("page", ArgSpec.greedy().suggest(this::pages)).executes(this::open));
		CommandUtil.contribute(this.ctx, CommandContribution.ROOT, node, "cc");
	}

	private CommandNode transfer(String name, boolean into) {
		ArgSpec half = ArgSpec.choice(List.of(TOP, BOTTOM));
		return CommandNode.literal(name).then(CommandNode.argument("page", ArgSpec.word().suggest(this::pages)).executes(context -> this.transfer(context, into))
			.then(CommandNode.argument("half", half).executes(context -> this.transfer(context, into))));
	}

	private List<String> pages() {
		List<String> pages = new ArrayList<>();
		for (String name : this.chest.store().names().values()) {
			pages.add(name);
		}
		for (int page : this.chest.store().filled()) {
			pages.add(String.valueOf(page + 1));
		}
		return pages;
	}

	private int page(CommandContext context) throws CommandException {
		Integer page = this.chest.resolve(context.string("page"));
		if (page == null) {
			throw new CommandException(this.ctx.tr("command.unknown_page", context.string("page")));
		}
		return page;
	}

	private void open(CommandContext context) throws CommandException {
		int page = this.page(context);
		CommandUtil.later(this.ctx, () -> this.chest.open(page));
	}

	private void transfer(CommandContext context, boolean into) throws CommandException {
		int page = this.page(context);
		String half = context.has("half") ? context.string("half") : null;
		Target target = CommandUtil.targetedOrHeld(this.ctx);
		if (into && !target.can(TargetCapability.WRITE)) {
			throw new CommandException(CommandUtil.error("read_only"));
		}
		ContainerService containers = CommandUtil.service(this.ctx, ContainerService.class);
		GameVersion version = target.dataVersion() == null ? CommandUtil.version(this.ctx) : target.dataVersion();
		target.load().thenCombine(this.chest.read(page), (root, items) -> {
			this.ctx.platform().scheduler().client(() -> this.apply(context, containers, target, version, root, items, page, half, into));
			return null;
		}).exceptionally(failure -> {
			this.ctx.platform().scheduler().client(() -> context.error(ErrorUtil.message(failure)));
			return null;
		});
	}

	private void apply(CommandContext context, ContainerService containers, Target target, GameVersion version, CompoundTag root, List<CompoundTag> page, int index, String half, boolean into) {
		ContainerSubject subject = containers.subject(target.kind(), root, version);
		Optional<ContainerFormat> found = containers.format(subject);
		if (found.isEmpty()) {
			context.error(this.ctx.tr("command.not_container", target.displayName()));
			return;
		}
		ContainerFormat format = found.get();
		List<CompoundTag> slots = new ArrayList<>(format.read(subject));
		int size = slots.size();
		int offset = BOTTOM.equals(half) ? ClientChestStore.SIZE - size : 0;
		if (size > (half == null ? ClientChestStore.SIZE : ClientChestStore.SIZE / 2)) {
			context.error(this.ctx.tr("command.too_big", size));
			return;
		}
		if (into) {
			for (int slot = 0; slot < size; slot++) {
				CompoundTag incoming = page.get(offset + slot);
				CompoundTag current = slots.get(slot);
				if (!ItemStackUtil.isEmpty(current) && !current.equals(incoming)) {
					this.ctx.hooks().fire(CoreHooks.ITEM_LOST, new ItemLostEvent(this.ctx.client().items().fromTag(current), LostItemReason.OVERWRITTEN));
				}
				slots.set(slot, incoming.copy());
			}
			CompoundTag next = root.copy();
			format.write(subject.withRoot(next), slots);
			CommandUtil.save(this.ctx, context, target, next, this.ctx.tr("command.imported", this.chest.label(index), target.displayName()));
			return;
		}
		List<CompoundTag> items = new ArrayList<>(page);
		if (half == null) {
			for (int slot = 0; slot < items.size(); slot++) {
				items.set(slot, new CompoundTag());
			}
		}
		for (int slot = 0; slot < size; slot++) {
			CompoundTag outgoing = slots.get(slot);
			items.set(offset + slot, ItemStackUtil.isEmpty(outgoing) ? new CompoundTag() : outgoing.copy());
		}
		this.chest.write(index, items).whenComplete((written, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
			} else if (!written) {
				context.error(this.ctx.tr("command.locked", this.chest.label(index)));
			} else {
				context.feedback(this.ctx.tr("command.exported", target.displayName(), this.chest.label(index)));
			}
		}));
	}
}
