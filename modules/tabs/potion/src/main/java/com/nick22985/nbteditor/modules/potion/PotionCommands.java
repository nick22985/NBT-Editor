package com.nick22985.nbteditor.modules.potion;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.PotionContents;
import com.nick22985.nbteditor.data.PotionEffect;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.PotionEffectUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotionCommands {
	private static final int DEFAULT_DURATION = 100;
	private static final int SOUP_SECONDS = 5;
	private static final String FLAG = "-";
	private static final List<String> FLAGS = List.of("-duration:", "-amplifier:", "-ambient", "-permanent", "-showparticles:", "-showicon:");

	private final ModuleContext ctx;
	private final Map<String, String> types = new LinkedHashMap<>();

	public PotionCommands(ModuleContext ctx) {
		this.ctx = ctx;
		this.types.put("normal", "minecraft:potion");
		this.types.put("splash", "minecraft:splash_potion");
		this.types.put("lingering", "minecraft:lingering_potion");
		this.types.put("arrow", "minecraft:tipped_arrow");
	}

	public void register() {
		ArgSpec effects = ArgSpec.greedy().suggest(this::suggestEffects);
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("potion").then(CommandNode.argument("type", ArgSpec.choice(List.copyOf(this.types.keySet()))).executes(this::potion).then(CommandNode.argument("effects", effects).executes(this::potion))), "p");
		ArgSpec effect = ArgSpec.id().suggest(() -> this.ctx.client().registries().ids(RegistryService.MOB_EFFECT));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("soup").then(CommandNode.argument("effect", effect).executes(this::soup).then(CommandNode.argument("duration", ArgSpec.integer(0, Integer.MAX_VALUE / 20)).executes(this::soup))), "stew");
	}

	private GameVersion version() {
		return CommandUtil.version(this.ctx);
	}

	private void potion(CommandContext context) throws CommandException {
		String type = context.string("type");
		String item = this.types.get(type.toLowerCase(Locale.ROOT));
		if (item == null) {
			throw new CommandException(this.ctx.tr("command.potion.unknown_type", type));
		}
		List<PotionEffect> effects = context.has("effects") ? this.parse(context.string("effects")) : List.of();
		GameVersion version = this.version();
		CompoundTag tag = ItemStackUtil.create(item, 1, version);
		ItemFields.POTION_CONTENTS.write(tag, version, PotionContents.EMPTY.withEffects(effects));
		CommandUtil.give(this.ctx, context, tag, this.ctx.tr("command.potion.given"));
	}

	private void soup(CommandContext context) throws CommandException {
		String id = this.effectId(context.string("effect"));
		int seconds = context.has("duration") ? context.integer("duration") : SOUP_SECONDS;
		GameVersion version = this.version();
		CompoundTag tag = ItemStackUtil.create(PotionPane.STEW, 1, version);
		ItemFields.STEW_EFFECTS.write(tag, version, List.of(PotionEffect.create(id).withDuration(seconds * 20)));
		CommandUtil.give(this.ctx, context, tag, this.ctx.tr("command.soup.given"));
	}

	private String effectId(String id) throws CommandException {
		String full = ItemStackUtil.namespaced(id);
		if (!this.ctx.client().registries().contains(RegistryService.MOB_EFFECT, full)) {
			throw new CommandException(this.ctx.tr("command.potion.unknown_effect", id));
		}
		return full;
	}

	private List<PotionEffect> parse(String input) throws CommandException {
		List<PotionEffect> effects = new ArrayList<>();
		for (String token : input.trim().split("\\s+")) {
			if (token.isEmpty()) {
				continue;
			}
			if (!token.startsWith(FLAG)) {
				effects.add(PotionEffect.create(this.effectId(token)).withDuration(DEFAULT_DURATION));
				continue;
			}
			if (effects.isEmpty()) {
				throw new CommandException(this.ctx.tr("command.potion.flag_first", token));
			}
			int last = effects.size() - 1;
			effects.set(last, this.flag(effects.get(last), token));
		}
		return effects;
	}

	private PotionEffect flag(PotionEffect effect, String token) throws CommandException {
		int colon = token.indexOf(':');
		String name = (colon < 0 ? token.substring(FLAG.length()) : token.substring(FLAG.length(), colon)).toLowerCase(Locale.ROOT);
		String value = colon < 0 ? null : token.substring(colon + 1);
		switch (name) {
			case "duration":
				Integer ticks = value == null ? null : PotionEffectUtil.parseDuration(value, Feature.INFINITE_EFFECTS.in(this.version()));
				if (ticks == null) {
					throw new CommandException(this.ctx.tr("command.potion.bad_value", token));
				}
				return effect.withDuration(ticks);
			case "amplifier":
				try {
					return effect.withAmplifier(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					throw new CommandException(this.ctx.tr("command.potion.bad_value", token));
				}
			case "permanent":
				return effect.withDuration(Feature.INFINITE_EFFECTS.in(this.version()) ? PotionEffect.INFINITE : Integer.MAX_VALUE);
			case "ambient":
				return effect.withAmbient(this.bool(value, token));
			case "showparticles":
				return effect.withShowParticles(this.bool(value, token));
			case "showicon":
				return effect.withShowIcon(this.bool(value, token));
			default:
				throw new CommandException(this.ctx.tr("command.potion.unknown_flag", token));
		}
	}

	private boolean bool(String value, String token) throws CommandException {
		if (value == null || value.equalsIgnoreCase("true")) {
			return true;
		}
		if (value.equalsIgnoreCase("false")) {
			return false;
		}
		throw new CommandException(this.ctx.tr("command.potion.bad_value", token));
	}

	private List<String> suggestEffects(CommandContext context, String remaining) {
		int space = remaining.lastIndexOf(' ');
		String prefix = remaining.substring(0, space + 1);
		List<String> values = new ArrayList<>();
		for (String id : this.ctx.client().registries().ids(RegistryService.MOB_EFFECT)) {
			values.add(prefix + id);
			if (space >= 0) {
				values.add(prefix + ItemStackUtil.stripNamespace(id));
			}
		}
		if (space >= 0) {
			for (String flag : FLAGS) {
				values.add(prefix + flag);
			}
		}
		return values;
	}
}
