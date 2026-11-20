package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandExecutor;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.command.HelpTopic;
import com.nick22985.nbteditor.command.TagMutation;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.platform.Hit;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.platform.Position;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldTargets;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandUtil {
	private static final int USAGE_DEPTH = 4;
	private static final int OPEN_DELAY = 1;
	private static final double REACH = 16;
	private static final String RELATIVE = "~";

	private CommandUtil() {
	}

	public static CommandContribution contribute(ModuleContext ctx, String group, CommandNode node) {
		return contribute(ctx, group, node, null);
	}

	public static CommandContribution contribute(ModuleContext ctx, String group, CommandNode node, String alias) {
		return contribute(ctx, group, node, alias, ctx.tr("command." + node.name()));
	}

	public static CommandContribution contribute(ModuleContext ctx, String group, CommandNode node, String alias, Text description) {
		CommandContribution contribution = new CommandContribution(group, node, description, alias);
		return ctx.register(CommandContribution.REGISTRY, contribution.id(), contribution);
	}

	public static HelpTopic helpTopic(ModuleContext ctx, String group, String name, Supplier<List<Text>> lines) {
		HelpTopic topic = new HelpTopic(group, name, ctx.tr("help." + name), lines);
		return ctx.register(HelpTopic.REGISTRY, topic.id(), topic);
	}

	public static String usage(CommandNode node) {
		return label(node) + suffix(node, 0);
	}

	public static String label(CommandNode node) {
		return node.isLiteral() ? node.name() : "<" + node.name() + ">";
	}

	private static String suffix(CommandNode node, int depth) {
		List<CommandNode> children = node.children();
		if (children.isEmpty() || depth >= USAGE_DEPTH) {
			return "";
		}
		String inner;
		if (children.size() == 1) {
			inner = label(children.get(0)) + suffix(children.get(0), depth + 1);
		} else {
			List<String> labels = new ArrayList<>();
			for (CommandNode child : children) {
				labels.add(label(child));
			}
			inner = String.join("|", labels);
		}
		if (node.executor() != null) {
			return " [" + inner + "]";
		}
		return children.size() == 1 ? " " + inner : " (" + inner + ")";
	}

	public static CommandNode coordinates(CommandExecutor executor, CommandNode... next) {
		CommandNode z = CommandNode.argument("z", ArgSpec.word().suggest(() -> List.of(RELATIVE)));
		if (executor != null) {
			z.executes(executor);
		}
		for (CommandNode child : next) {
			z.then(child);
		}
		return CommandNode.argument("x", ArgSpec.word().suggest(() -> List.of(RELATIVE))).then(CommandNode.argument("y", ArgSpec.word().suggest(() -> List.of(RELATIVE))).then(z));
	}

	public static Position position(ModuleContext ctx, CommandContext context) throws CommandException {
		Position base = ctx.client().player().position();
		return new Position(coordinate(context, "x", base.x()), coordinate(context, "y", base.y()), coordinate(context, "z", base.z()));
	}

	private static double coordinate(CommandContext context, String name, double base) throws CommandException {
		String value = context.string(name);
		try {
			if (value.startsWith(RELATIVE)) {
				return value.length() == 1 ? base : base + Double.parseDouble(value.substring(1));
			}
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new CommandException(error("bad_coordinate", value));
		}
	}

	public static Text error(String key, Object... args) {
		return Text.translatable("nbtedit.command." + key, args);
	}

	public static GameVersion version(ModuleContext ctx) {
		return ctx.platform().game().version();
	}

	public static SnbtDialect dialect(GameVersion version) {
		return Feature.MODERN_SNBT.in(version) ? SnbtDialect.MODERN : SnbtDialect.LEGACY;
	}

	public static <T> T service(ModuleContext ctx, Class<T> type) throws CommandException {
		Optional<T> found = ctx.services().find(type);
		if (found.isEmpty()) {
			throw new CommandException(error("unavailable"));
		}
		return found.get();
	}

	public static void later(ModuleContext ctx, Runnable task) {
		ctx.platform().scheduler().later(OPEN_DELAY, task);
	}

	public static GameItem heldItem(ModuleContext ctx) throws CommandException {
		GameItem item = ctx.client().player().held(Hand.MAIN);
		if (!InventoryUtil.present(item)) {
			throw new CommandException(error("no_item"));
		}
		return item;
	}

	public static Target heldTarget(ModuleContext ctx) throws CommandException {
		heldItem(ctx);
		return service(ctx, ItemTargets.class).held(Hand.MAIN);
	}

	public static Optional<Target> targeted(ModuleContext ctx) {
		Optional<WorldTargets> world = ctx.services().find(WorldTargets.class);
		if (world.isEmpty()) {
			return Optional.empty();
		}
		Hit hit = ctx.client().world().targeted(REACH, false);
		return hit == null ? Optional.empty() : world.get().hit(hit);
	}

	public static Target heldOrTargeted(ModuleContext ctx) throws CommandException {
		if (InventoryUtil.present(ctx.client().player().held(Hand.MAIN))) {
			return service(ctx, ItemTargets.class).held(Hand.MAIN);
		}
		Optional<Target> target = targeted(ctx);
		if (target.isPresent()) {
			return target.get();
		}
		throw new CommandException(error("no_target"));
	}

	public static Target targetedOrHeld(ModuleContext ctx) throws CommandException {
		Optional<Target> target = targeted(ctx);
		if (target.isPresent()) {
			return target.get();
		}
		if (InventoryUtil.present(ctx.client().player().held(Hand.MAIN))) {
			return service(ctx, ItemTargets.class).held(Hand.MAIN);
		}
		throw new CommandException(error("no_target"));
	}

	public static Target newHeld(ModuleContext ctx, String id) throws CommandException {
		PlayerService player = ctx.client().player();
		if (!player.creative()) {
			throw new CommandException(error("creative"));
		}
		if (InventoryUtil.present(player.held(Hand.MAIN))) {
			throw new CommandException(error("hand_occupied"));
		}
		if (!ctx.client().items().isItem(id)) {
			throw new CommandException(error("invalid_item"));
		}
		GameItem item = ctx.client().items().fromTag(ItemStackUtil.create(id, 1, version(ctx)));
		if (!player.setSlot(player.selectedSlot(), item)) {
			throw new CommandException(error("invalid_item"));
		}
		return service(ctx, ItemTargets.class).held(Hand.MAIN);
	}

	public static void editHeld(ModuleContext ctx, CommandContext command, TagMutation mutation, Text done) throws CommandException {
		GameItem item = heldItem(ctx);
		Target target = service(ctx, ItemTargets.class).held(Hand.MAIN);
		if (!target.can(TargetCapability.WRITE)) {
			throw new CommandException(error("read_only"));
		}
		CompoundTag tag = item.tag().copy();
		mutation.apply(tag);
		save(ctx, command, target, tag, done);
	}

	public static void save(ModuleContext ctx, CommandContext command, Target target, CompoundTag tag, Text done) {
		target.save(tag).whenComplete((ignored, failure) -> ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				command.error(ErrorUtil.message(failure));
			} else if (done != null) {
				command.feedback(done);
			}
		}));
	}

	public static void give(ModuleContext ctx, CommandContext command, CompoundTag tag, Text done) throws CommandException {
		if (!ctx.client().player().creative()) {
			throw new CommandException(error("creative"));
		}
		GameItem item = ctx.client().items().fromTag(tag);
		if (!InventoryUtil.present(item)) {
			throw new CommandException(error("invalid_item"));
		}
		if (!ctx.client().player().give(item)) {
			throw new CommandException(error("inventory_full"));
		}
		if (done != null) {
			command.feedback(done);
		}
	}

	public static void openEditor(ModuleContext ctx, Target target, String tab) throws CommandException {
		EditorService editor = service(ctx, EditorService.class);
		later(ctx, () -> editor.open(target, tab));
	}
}
