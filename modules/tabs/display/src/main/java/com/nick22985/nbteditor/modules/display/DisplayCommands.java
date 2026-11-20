package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.Field;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.text.ClickAction;
import com.nick22985.nbteditor.text.ClickEvent;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.HoverEvent;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DisplayCommands {
	private static final String MAX_STACK_SIZE = "max_stack_size";
	private static final String GLINT = "enchantment_glint_override";
	private static final int STACK_LIMIT = 99;
	private static final String LORE_COMMAND = "/" + CommandContribution.FACTORY + " lore ";

	private final ModuleContext ctx;

	public DisplayCommands(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("name").executes(context -> this.name(context, null))
			.then(CommandNode.argument("text", ArgSpec.greedy()).executes(context -> this.name(context, this.parse(context.string("text"))))), "n");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("lore").executes(this::listLore)
			.then(CommandNode.literal("list").executes(this::listLore))
			.then(CommandNode.literal("add").then(CommandNode.argument("text", ArgSpec.greedy()).executes(context -> this.lore(context, lines -> lines.add(this.parse(context.string("text")))))))
			.then(CommandNode.literal("set").then(CommandNode.argument("line", ArgSpec.integer(1, Integer.MAX_VALUE)).then(CommandNode.argument("text", ArgSpec.greedy()).executes(context -> this.lore(context, lines -> lines.set(this.line(context), this.parse(context.string("text"))))))))
			.then(CommandNode.literal("insert").then(CommandNode.argument("line", ArgSpec.integer(1, Integer.MAX_VALUE)).then(CommandNode.argument("text", ArgSpec.greedy()).executes(context -> this.lore(context, lines -> lines.add(this.line(context), this.parse(context.string("text"))))))))
			.then(CommandNode.literal("remove").then(CommandNode.argument("line", ArgSpec.integer(1, Integer.MAX_VALUE)).executes(context -> this.lore(context, lines -> lines.remove(this.line(context))))))
			.then(CommandNode.literal("clear").executes(context -> this.lore(context, List::clear))), "l");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("unbreakable").executes(context -> this.unbreakable(context, null))
			.then(CommandNode.argument("value", ArgSpec.bool()).executes(context -> this.unbreakable(context, context.bool("value")))), "ub");
		if (Feature.ITEM_COMPONENTS.in(CommandUtil.version(this.ctx))) {
			CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("maxstacksize").executes(context -> this.maxStackSize(context, 0))
				.then(CommandNode.argument("size", ArgSpec.integer(1, STACK_LIMIT)).executes(context -> this.maxStackSize(context, context.integer("size")))), "mss");
			CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("glint").executes(context -> this.glint(context, null))
				.then(CommandNode.argument("value", ArgSpec.bool()).executes(context -> this.glint(context, context.bool("value")))), "gl");
		}
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("amount")
			.then(CommandNode.argument("count", ArgSpec.integer(1, ItemStackUtil.maxCount(CommandUtil.version(this.ctx)))).executes(this::amount)), "am");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("damage").executes(context -> this.number(context, ItemFields.DAMAGE, null, "damage"))
			.then(CommandNode.argument("value", ArgSpec.integer(0, Integer.MAX_VALUE)).executes(context -> this.number(context, ItemFields.DAMAGE, context.integer("value"), "damage"))), "dmg");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("model").executes(context -> this.number(context, ItemFields.CUSTOM_MODEL_DATA, null, "model"))
			.then(CommandNode.argument("value", ArgSpec.integer()).executes(context -> this.number(context, ItemFields.CUSTOM_MODEL_DATA, context.integer("value"), "model"))), "cmd");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("repaircost").executes(context -> this.number(context, ItemFields.REPAIR_COST, null, "repaircost"))
			.then(CommandNode.argument("value", ArgSpec.integer(0, Integer.MAX_VALUE)).executes(context -> this.number(context, ItemFields.REPAIR_COST, context.integer("value"), "repaircost"))), "rc");
		CommandUtil.contribute(this.ctx, CommandContribution.FACTORY, CommandNode.literal("color").executes(context -> this.color(context, null))
			.then(CommandNode.argument("hex", ArgSpec.word()).executes(context -> this.color(context, context.string("hex")))), "col");
	}

	private Text parse(String input) {
		TextSyntax syntax = this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
		return syntax.parse(input);
	}

	private void name(CommandContext context, Text name) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		CommandUtil.editHeld(this.ctx, context, tag -> ItemFields.CUSTOM_NAME.write(tag, version, name), this.ctx.tr(name == null ? "command.name.cleared" : "command.name.set"));
	}

	private int line(CommandContext context) {
		return context.integer("line") - 1;
	}

	private void lore(CommandContext context, Consumer<List<Text>> change) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		CommandUtil.editHeld(this.ctx, context, tag -> {
			List<Text> current = ItemFields.LORE.get(tag, version);
			List<Text> lines = current == null ? new ArrayList<>() : new ArrayList<>(current);
			try {
				change.accept(lines);
			} catch (IndexOutOfBoundsException e) {
				throw new CommandException(this.ctx.tr("command.lore.no_line", context.integer("line"), lines.size()));
			}
			ItemFields.LORE.write(tag, version, lines.isEmpty() ? null : lines);
		}, this.ctx.tr("command.lore.done"));
	}

	private void listLore(CommandContext context) throws CommandException {
		List<Text> lines = ItemFields.LORE.get(CommandUtil.heldItem(this.ctx).tag(), CommandUtil.version(this.ctx));
		context.feedback(Text.join(List.of(this.action(Text.literal("+"), Formatting.GREEN, LORE_COMMAND + "add "), Text.literal(" "), this.action(this.ctx.tr("command.lore.clear"), Formatting.RED, LORE_COMMAND + "clear"))));
		if (lines == null || lines.isEmpty()) {
			context.feedback(this.ctx.tr("command.lore.none"));
			return;
		}
		TextSyntax syntax = this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
		for (int i = 0; i < lines.size(); i++) {
			int line = i + 1;
			String set = LORE_COMMAND + "set " + line + " " + syntax.stringify(lines.get(i));
			Style edit = Style.EMPTY.withClickEvent(new ClickEvent(ClickAction.SUGGEST_COMMAND, set)).withHoverEvent(HoverEvent.showText(this.ctx.tr("command.lore.edit", line)));
			context.feedback(Text.join(List.of(this.action(Text.literal("-"), Formatting.RED, LORE_COMMAND + "remove " + line), Text.literal(" " + line + ". ", Style.EMPTY.withColor(Formatting.GRAY)), Text.join(List.of(lines.get(i))).withStyle(edit))));
		}
	}

	private Text action(Text label, Formatting color, String command) {
		Style style = Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickAction.SUGGEST_COMMAND, command)).withHoverEvent(HoverEvent.showText(Text.literal(command)));
		return Text.join(List.of(Text.literal("["), label.withStyle(Style.EMPTY.withColor(color)), Text.literal("]"))).withStyle(style);
	}

	private void unbreakable(CommandContext context, Boolean value) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		boolean next = value == null ? !Boolean.TRUE.equals(ItemFields.UNBREAKABLE.get(CommandUtil.heldItem(this.ctx).tag(), version)) : value;
		CommandUtil.editHeld(this.ctx, context, tag -> ItemFields.UNBREAKABLE.write(tag, version, next), this.ctx.tr(next ? "command.unbreakable.on" : "command.unbreakable.off"));
	}

	private void amount(CommandContext context) throws CommandException {
		int count = context.integer("count");
		GameVersion version = CommandUtil.version(this.ctx);
		CommandUtil.editHeld(this.ctx, context, tag -> ItemStackUtil.setCount(tag, count, version), this.ctx.tr("command.amount.set", count));
	}

	private void number(CommandContext context, Field<Integer> field, Integer value, String name) throws CommandException {
		GameVersion version = CommandUtil.version(this.ctx);
		CommandUtil.editHeld(this.ctx, context, tag -> field.write(tag, version, value), value == null ? this.ctx.tr("command." + name + ".reset") : this.ctx.tr("command." + name + ".set", value));
	}

	private void color(CommandContext context, String hex) throws CommandException {
		Integer color = hex == null ? null : ColorUtil.parseHex(hex);
		if (hex != null && color == null) {
			throw new CommandException(this.ctx.tr("command.color.invalid", hex));
		}
		GameVersion version = CommandUtil.version(this.ctx);
		CommandUtil.editHeld(this.ctx, context, tag -> ItemFields.DYED_COLOR.write(tag, version, color), color == null ? this.ctx.tr("command.color.reset") : this.ctx.tr("command.color.set", ColorUtil.toHex(color)));
	}

	private void glint(CommandContext context, Boolean value) throws CommandException {
		CommandUtil.editHeld(this.ctx, context, tag -> ItemStackUtil.setComponent(tag, GLINT, value == null ? null : ByteTag.of(value)), value == null ? this.ctx.tr("command.glint.reset") : this.ctx.tr(value ? "command.glint.on" : "command.glint.off"));
	}

	private void maxStackSize(CommandContext context, int size) throws CommandException {
		CommandUtil.editHeld(this.ctx, context, tag -> ItemStackUtil.setComponent(tag, MAX_STACK_SIZE, size <= 0 ? null : IntTag.of(size)), size <= 0 ? this.ctx.tr("command.maxstacksize.reset") : this.ctx.tr("command.maxstacksize.set", size));
	}
}
