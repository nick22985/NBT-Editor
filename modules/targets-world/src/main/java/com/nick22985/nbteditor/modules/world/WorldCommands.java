package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.command.ArgSpec;
import com.nick22985.nbteditor.command.CommandContext;
import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.DoubleTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.platform.Position;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.target.WorldBackends;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockDocUtil;
import com.nick22985.nbteditor.util.BlockStateUtil;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.PickItemUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.List;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class WorldCommands {
	private final ModuleContext ctx;
	private final WorldTargetFactory targets;

	public WorldCommands(ModuleContext ctx, WorldTargetFactory targets) {
		this.ctx = ctx;
		this.targets = targets;
	}

	public void register() {
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("self").executes(context -> CommandUtil.openEditor(this.ctx, this.targets.self(), null)), "me");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("storage").then(CommandNode.argument("id", ArgSpec.id()).executes(context -> CommandUtil.openEditor(this.ctx, this.targets.storage(context.string("id")), null))), "st");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("gamerules").executes(context -> CommandUtil.openEditor(this.ctx, this.targets.gameRules(), null)), "gr");
		CommandUtil.contribute(this.ctx, CommandContribution.NBT, CommandNode.literal("block").then(CommandUtil.coordinates(context -> CommandUtil.openEditor(this.ctx, this.targets.block(this.dimension(), CommandUtil.position(this.ctx, context).block()), null))), "b");
		ArgSpec block = ArgSpec.greedy().suggest(() -> this.ids(RegistryService.BLOCK));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("block").executes(this::getTargetedBlock)
			.then(CommandNode.literal("place").then(CommandUtil.coordinates(null, CommandNode.argument("block", block).executes(context -> this.placeBlock(context)))))
			.then(CommandNode.argument("block", block).executes(this::getBlock)), "b", this.ctx.tr("command.get_block"));
		ArgSpec entity = ArgSpec.id().suggest(() -> this.ids(RegistryService.ENTITY_TYPE));
		CommandUtil.contribute(this.ctx, CommandContribution.GET, CommandNode.literal("entity").executes(this::getTargetedEntity)
			.then(CommandNode.literal("summon").then(CommandUtil.coordinates(null, CommandNode.argument("type", entity).executes(this::summon).then(CommandNode.argument("snbt", ArgSpec.greedy()).executes(this::summon)))))
			.then(CommandNode.argument("type", entity).executes(this::getEntity).then(CommandNode.argument("snbt", ArgSpec.greedy()).executes(this::getEntity))), "e");
	}

	private List<String> ids(String registry) {
		return this.ctx.client().registries().ids(registry);
	}

	private String dimension() {
		return this.ctx.client().world().dimension();
	}

	private GameVersion version() {
		return CommandUtil.version(this.ctx);
	}

	private BlockData parseBlock(CommandContext context) throws CommandException {
		BlockData data;
		try {
			data = BlockDocUtil.parseSpec(context.string("block"), CommandUtil.dialect(this.version()));
		} catch (SnbtException e) {
			throw new CommandException(CommandUtil.error("invalid_snbt", e.getMessage()));
		}
		if (!this.ctx.client().registries().contains(RegistryService.BLOCK, data.blockId())) {
			throw new CommandException(this.ctx.tr("command.unknown_block", data.blockId()));
		}
		return data;
	}

	private void giveBlock(CommandContext context, BlockData data) throws CommandException {
		RegistryService registries = this.ctx.client().registries();
		String item = registries.blockItem(data.blockId());
		if (item == null) {
			throw new CommandException(this.ctx.tr("command.no_block_item", registries.name(RegistryService.BLOCK, data.blockId())));
		}
		CompoundTag tag = PickItemUtil.block(data, item, registries.defaultBlockState(data.blockId()), this.version());
		CommandUtil.give(this.ctx, context, tag, this.ctx.tr("command.given", registries.name(RegistryService.BLOCK, data.blockId())));
	}

	private void getBlock(CommandContext context) throws CommandException {
		this.giveBlock(context, this.parseBlock(context));
	}

	private void getTargetedBlock(CommandContext context) throws CommandException {
		Target target = this.targeted(SubjectKind.BLOCK);
		target.load().whenComplete((tag, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
				return;
			}
			try {
				this.giveBlock(context, BlockDocUtil.fromTag(tag));
			} catch (CommandException e) {
				context.error(e.text());
			}
		}));
	}

	private void placeBlock(CommandContext context) throws CommandException {
		BlockData data = this.parseBlock(context);
		Position at = CommandUtil.position(this.ctx, context);
		WorldBackend backend = this.backend(SubjectKind.BLOCK);
		backend.writeBlock(this.dimension(), at.block(), BlockStateUtil.sync(data), BlockWriteFlags.DEFAULT).whenComplete((ignored, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
			} else {
				context.feedback(this.ctx.tr("command.placed", this.ctx.client().registries().name(RegistryService.BLOCK, data.blockId())));
			}
		}));
	}

	private CompoundTag parseEntity(CommandContext context) throws CommandException {
		String type = context.string("type");
		if (!this.ctx.client().registries().contains(RegistryService.ENTITY_TYPE, type)) {
			throw new CommandException(this.ctx.tr("command.unknown_entity", type));
		}
		CompoundTag tag;
		try {
			tag = context.has("snbt") ? SnbtUtil.parseCompound(context.string("snbt"), CommandUtil.dialect(this.version())) : new CompoundTag();
		} catch (SnbtException e) {
			throw new CommandException(CommandUtil.error("invalid_snbt", e.getMessage()));
		}
		tag.putString("id", type);
		return tag;
	}

	private void giveEntity(CommandContext context, CompoundTag entity) throws CommandException {
		String type = entity.getString("id", "");
		Text name = this.ctx.client().registries().name(RegistryService.ENTITY_TYPE, type);
		String item = PickItemUtil.entityItem(type, entity, this.ctx.client().items()::isItem);
		if (item == null) {
			throw new CommandException(this.ctx.tr("command.no_entity_item", name));
		}
		CommandUtil.give(this.ctx, context, PickItemUtil.entity(type, entity, item, this.version()), this.ctx.tr("command.given", name));
	}

	private void getEntity(CommandContext context) throws CommandException {
		this.giveEntity(context, this.parseEntity(context));
	}

	private void getTargetedEntity(CommandContext context) throws CommandException {
		Target target = this.targeted(SubjectKind.ENTITY);
		target.load().whenComplete((tag, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
				return;
			}
			try {
				this.giveEntity(context, tag);
			} catch (CommandException e) {
				context.error(e.text());
			}
		}));
	}

	private void summon(CommandContext context) throws CommandException {
		CompoundTag entity = PickItemUtil.summonable(this.parseEntity(context));
		Position at = CommandUtil.position(this.ctx, context);
		ListTag pos = new ListTag();
		pos.add(DoubleTag.of(at.x()));
		pos.add(DoubleTag.of(at.y()));
		pos.add(DoubleTag.of(at.z()));
		entity.put("Pos", pos);
		Text name = this.ctx.client().registries().name(RegistryService.ENTITY_TYPE, entity.getString("id", ""));
		this.backend(SubjectKind.ENTITY).summonEntity(this.dimension(), entity).whenComplete((ignored, failure) -> this.ctx.platform().scheduler().client(() -> {
			if (failure != null) {
				context.error(ErrorUtil.message(failure));
			} else {
				context.feedback(this.ctx.tr("command.summoned", name));
			}
		}));
	}

	private Target targeted(SubjectKind kind) throws CommandException {
		Optional<Target> target = CommandUtil.targeted(this.ctx);
		if (target.isEmpty() || target.get().kind() != kind) {
			throw new CommandException(this.ctx.tr(kind == SubjectKind.BLOCK ? "command.no_block" : "command.no_entity"));
		}
		return target.get();
	}

	private WorldBackend backend(SubjectKind kind) throws CommandException {
		Optional<WorldBackend> backend = this.ctx.services().get(WorldBackends.class).best(kind, TargetCapability.WRITE);
		if (backend.isEmpty()) {
			throw new CommandException(this.ctx.tr("no_backend"));
		}
		return backend.get();
	}
}
