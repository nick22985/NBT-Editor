package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.EntityTrait;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldTargets;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerOpener implements ContainerService {
	private static final String SPAWN_EGG = "_spawn_egg";

	private final ModuleContext ctx;
	private final Function<String, Set<EntityTrait>> traits;

	public ContainerOpener(ModuleContext ctx, Function<String, Set<EntityTrait>> traits) {
		this.ctx = ctx;
		this.traits = traits;
	}

	public GameVersion version(Target target) {
		GameVersion version = target.dataVersion();
		return version == null ? this.ctx.platform().game().version() : version;
	}

	@Override
	public ContainerSubject subject(SubjectKind kind, CompoundTag root, GameVersion version) {
		String id;
		String entity = null;
		if (kind == SubjectKind.ITEM) {
			id = ItemStackUtil.id(root);
			entity = this.itemEntity(id, root, version);
		} else if (kind == SubjectKind.BLOCK) {
			id = BlockEntityUtil.blockId(root, false);
		} else if (kind == SubjectKind.ENTITY) {
			id = ItemStackUtil.namespaced(root.getString("id", ""));
			entity = id;
		} else {
			id = "";
		}
		Set<EntityTrait> traits = entity == null ? Set.of() : this.traits.apply(entity);
		return new ContainerSubject(kind, id, entity, traits, root, version);
	}

	private String itemEntity(String id, CompoundTag root, GameVersion version) {
		String path = ItemStackUtil.stripNamespace(id);
		boolean spawnEgg = path.endsWith(SPAWN_EGG);
		boolean vehicle = path.equals("chest_minecart") || path.equals("hopper_minecart") || path.endsWith("_chest_boat") || path.endsWith("_chest_raft");
		if (!spawnEgg && !path.equals("armor_stand") && !(vehicle && Feature.CONTAINER_ITEMS_IN_ENTITIES.in(version))) {
			return null;
		}
		Tag data = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(root, EntityHost.ITEM_COMPONENT) : ItemDataUtil.dataTag(root, version, EntityHost.ITEM_LEGACY);
		if (data instanceof CompoundTag && ((CompoundTag) data).getString("id") != null) {
			return ItemStackUtil.namespaced(((CompoundTag) data).getString("id"));
		}
		if (spawnEgg) {
			return ItemStackUtil.namespaced(path.substring(0, path.length() - SPAWN_EGG.length()));
		}
		if ((path.endsWith("_chest_boat") || path.endsWith("_chest_raft")) && version.below(GameVersion.V1_21_2)) {
			return "minecraft:chest_boat";
		}
		return ItemStackUtil.namespaced(path);
	}

	@Override
	public Optional<ContainerFormat> format(ContainerSubject subject) {
		for (ContainerFormat format : this.ctx.registry(ContainerFormat.REGISTRY).values()) {
			if (format.supports(subject)) {
				return Optional.of(format);
			}
		}
		return Optional.empty();
	}

	@Override
	public void open(Target target) {
		this.open(target, target.displayName());
	}

	public void openEnderChest() {
		Optional<Target> self = this.ctx.services().find(WorldTargets.class).map(WorldTargets::self);
		if (self.isEmpty() || !self.get().can(TargetCapability.REMOTE)) {
			this.ctx.client().screens().toast(this.ctx.tr("open.failed"), this.ctx.tr("ender_chest.no_backend"));
			return;
		}
		this.open(self.get(), this.ctx.tr("ender_chest"));
	}

	public boolean enderChestAvailable() {
		return this.ctx.client().player().present() && this.ctx.services().find(WorldTargets.class).map(targets -> targets.self().can(TargetCapability.REMOTE)).orElse(false);
	}

	private void open(Target target, Text title) {
		target.load().whenComplete((tag, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null) {
				this.ctx.log().warn("Could not open container " + target.id(), ErrorUtil.unwrap(error));
				this.ctx.client().screens().toast(this.ctx.tr("open.failed"), ErrorUtil.message(error));
				return;
			}
			ContainerSubject subject = this.subject(target.kind(), tag, this.version(target));
			Optional<ContainerFormat> format = this.format(subject);
			if (format.isEmpty()) {
				this.ctx.client().screens().toast(this.ctx.tr("open.failed"), this.ctx.tr("unsupported"));
				return;
			}
			this.ctx.client().screens().open(new ContainerScreen(this.ctx, this, target, subject, format.get(), title));
		}));
	}
}
