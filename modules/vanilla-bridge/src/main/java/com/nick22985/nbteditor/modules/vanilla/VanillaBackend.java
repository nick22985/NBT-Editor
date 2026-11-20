package com.nick22985.nbteditor.modules.vanilla;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TranslatableContent;
import com.nick22985.nbteditor.util.DataCommandUtil;
import com.nick22985.nbteditor.util.ItemCommandUtil;
import com.nick22985.nbteditor.util.PlayerInventoryUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import com.nick22985.nbteditor.util.TextQueryUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class VanillaBackend implements WorldBackend {
	private static final Set<String> NONE = Set.of();
	private static final Set<String> UNCHANGED = Set.of("commands.data.merge.failed", "arguments.nbtpath.nothing_found", "commands.setblock.failed");
	private static final Set<String> RULE_UNCHANGED = Set.of("commands.gamerule.not_set");
	private static final List<String> BLOCK_ENTITY_IDENTITY = List.of("x", "y", "z", "id");

	private final ModuleContext ctx;
	private final CommandQueue queue;
	private final Setting<Boolean> enabled;
	private final Map<String, CompoundTag> snapshots = new ConcurrentHashMap<>();

	public VanillaBackend(ModuleContext ctx, CommandQueue queue, Setting<Boolean> enabled) {
		this.ctx = ctx;
		this.queue = queue;
		this.enabled = enabled;
	}

	public void reset() {
		this.snapshots.clear();
	}

	private GameVersion version() {
		return this.ctx.platform().game().version();
	}

	private SnbtDialect dialect() {
		return Feature.MODERN_SNBT.in(this.version()) ? SnbtDialect.MODERN : SnbtDialect.LEGACY;
	}

	private boolean knows(String root) {
		return this.ctx.client().commands().serverKnows(root);
	}

	@Override
	public String id() {
		return "vanilla";
	}

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public boolean available() {
		return this.enabled.get() && this.ctx.client().world().present() && this.knows("data");
	}

	@Override
	public Text name() {
		return this.ctx.tr("name");
	}

	@Override
	public boolean supports(SubjectKind kind, TargetCapability capability) {
		switch (kind) {
			case BLOCK:
			case ENTITY:
			case STORAGE:
				return true;
			case PLAYER:
				return capability != TargetCapability.WRITE || this.knows("item");
			case WORLD:
				return this.knows("gamerule") && this.queue.spamExempt();
			default:
				return false;
		}
	}

	private String prefix(String dimension) {
		return dimension == null || dimension.equals(this.ctx.client().world().dimension()) ? "" : "execute in " + dimension + " run ";
	}

	private static String blockTarget(BlockPos pos) {
		return "block " + pos.x() + " " + pos.y() + " " + pos.z();
	}

	private static String entityTarget(UUID uuid) {
		return "entity " + uuid;
	}

	private CompletableFuture<CompoundTag> query(String prefix, String target, String key) {
		String kind = target.substring(0, target.indexOf(' '));
		String reply = "commands.data." + kind + ".query";
		return this.queue.submit(prefix + "data get " + target, Set.of(reply), NONE).thenApply(message -> {
			TranslatableContent content = TextQueryUtil.firstTranslatable(message);
			String snbt = content == null || !content.key().equals(reply) ? null : TextQueryUtil.argText(content, -1);
			if (snbt == null) {
				throw new TargetException(this.ctx.tr("error.unexpected", message));
			}
			try {
				CompoundTag tag = SnbtUtil.parseCompound(snbt, this.dialect());
				this.snapshots.put(key, tag.copy());
				return tag;
			} catch (SnbtException e) {
				throw new TargetException(this.ctx.tr("error.parse", e.getMessage()), e);
			}
		});
	}

	private CompletableFuture<Void> run(List<String> commands, Set<String> success) {
		List<CompletableFuture<Text>> results = new ArrayList<>();
		for (String command : commands) {
			results.add(this.queue.submit(command, success, UNCHANGED));
		}
		return CompletableFuture.allOf(results.toArray(new CompletableFuture[0]));
	}

	private List<String> diff(String prefix, String target, CompoundTag before, CompoundTag after) {
		try {
			return DataCommandUtil.diff(prefix, target, before, after, this.dialect());
		} catch (IllegalArgumentException e) {
			throw new TargetException(this.ctx.tr("error." + e.getMessage()));
		}
	}

	private CompletableFuture<CompoundTag> snapshot(String key, Supplier<CompletableFuture<CompoundTag>> reader) {
		CompoundTag cached = this.snapshots.get(key);
		return cached != null ? CompletableFuture.completedFuture(cached.copy()) : reader.get();
	}

	private CompletableFuture<Void> writeData(String prefix, String target, String key, CompoundTag after) {
		return this.snapshot(key, () -> this.query(prefix, target, key)).thenCompose(before -> {
			List<String> commands = this.diff(prefix, target, before, after);
			String kind = target.substring(0, target.indexOf(' '));
			return this.run(commands, Set.of("commands.data." + kind + ".modified")).thenRun(() -> this.snapshots.put(key, after.copy()));
		});
	}

	private static CompoundTag withoutIdentity(CompoundTag tag) {
		CompoundTag copy = tag == null ? new CompoundTag() : tag.copy();
		for (String key : BLOCK_ENTITY_IDENTITY) {
			copy.remove(key);
		}
		return copy;
	}

	@Override
	public CompletableFuture<BlockData> readBlock(String dimension, BlockPos pos) {
		if (!this.prefix(dimension).isEmpty()) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("error.other_dimension")));
		}
		BlockData client = this.ctx.client().world().clientBlock(pos);
		if (client == null) {
			return CompletableFuture.completedFuture(null);
		}
		if (client.blockEntity() == null) {
			return CompletableFuture.completedFuture(client);
		}
		return this.query("", blockTarget(pos), "block:" + dimension + ":" + pos).thenApply(tag -> new BlockData(client.blockId(), client.properties(), tag));
	}

	@Override
	public CompletableFuture<Void> writeBlock(String dimension, BlockPos pos, BlockData data, BlockWriteFlags flags) {
		return this.readBlock(dimension, pos).thenCompose(current -> {
			List<String> commands = new ArrayList<>();
			CompoundTag before = current == null ? null : current.blockEntity();
			boolean recreate = flags != null && flags.recreate() && data.blockEntity() != null;
			boolean stateChanged = recreate || current == null || !current.stateString().equals(data.stateString());
			if (recreate) {
				commands.add("setblock " + pos.x() + " " + pos.y() + " " + pos.z() + " minecraft:air");
			}
			if (stateChanged) {
				commands.add("setblock " + pos.x() + " " + pos.y() + " " + pos.z() + " " + data.stateString());
				before = new CompoundTag();
			}
			if (data.blockEntity() != null) {
				commands.addAll(this.diff("", blockTarget(pos), withoutIdentity(before), withoutIdentity(data.blockEntity())));
			}
			for (String command : commands) {
				if (!DataCommandUtil.fits(command)) {
					throw new TargetException(this.ctx.tr("error.too_long"));
				}
			}
			this.snapshots.remove("block:" + dimension + ":" + pos);
			return this.run(commands, Set.of("commands.setblock.success", "commands.data.block.modified"));
		});
	}

	@Override
	public CompletableFuture<CompoundTag> readEntity(String dimension, UUID uuid) {
		return this.query(this.prefix(dimension), entityTarget(uuid), "entity:" + uuid);
	}

	@Override
	public CompletableFuture<Void> writeEntity(String dimension, UUID uuid, CompoundTag tag, boolean recreate) {
		CompoundTag after = tag.copy();
		after.remove("UUID");
		after.remove("id");
		String key = "entity:" + uuid;
		String prefix = this.prefix(dimension);
		return this.snapshot(key, () -> this.query(prefix, entityTarget(uuid), key)).thenCompose(before -> {
			CompoundTag previous = before.copy();
			previous.remove("UUID");
			previous.remove("id");
			List<String> commands = this.diff(prefix, entityTarget(uuid), previous, after);
			return this.run(commands, Set.of("commands.data.entity.modified")).thenRun(() -> this.snapshots.put(key, tag.copy()));
		});
	}

	@Override
	public CompletableFuture<Void> summonEntity(String dimension, CompoundTag tag) {
		CompoundTag data = tag.copy();
		String id = data.getString("id", "");
		ListTag pos = data.getList("Pos");
		data.remove("id");
		data.remove("Pos");
		data.remove("UUID");
		String at = pos != null && pos.size() == 3 ? " " + this.coordinate(pos.get(0)) + " " + this.coordinate(pos.get(1)) + " " + this.coordinate(pos.get(2)) : " ~ ~ ~";
		String command = this.prefix(dimension) + "summon " + id + at + (data.isEmpty() ? "" : " " + SnbtUtil.write(data, this.dialect()));
		if (!DataCommandUtil.fits(command)) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("error.too_long")));
		}
		return this.queue.submit(command, Set.of("commands.summon.success"), Set.of()).thenApply(message -> null);
	}

	private String coordinate(Tag tag) {
		return tag instanceof NumericTag ? String.valueOf(((NumericTag) tag).number().doubleValue()) : "~";
	}

	@Override
	public CompletableFuture<CompoundTag> readPlayer(UUID uuid) {
		return this.query("", entityTarget(uuid), "player:" + uuid);
	}

	@Override
	public CompletableFuture<Void> writePlayer(UUID uuid, CompoundTag tag) {
		String key = "player:" + uuid;
		return this.snapshot(key, () -> this.readPlayer(uuid)).thenCompose(before -> {
			if (!PlayerInventoryUtil.withoutInventory(before).equals(PlayerInventoryUtil.withoutInventory(tag))) {
				throw new TargetException(this.ctx.tr("error.player_data"));
			}
			List<String> commands = new ArrayList<>();
			for (Map.Entry<Integer, CompoundTag> entry : this.changedSlots(PlayerInventoryUtil.slots(before), PlayerInventoryUtil.slots(tag)).entrySet()) {
				commands.add(this.slotCommand(uuid, entry.getKey(), entry.getValue()));
			}
			for (Map.Entry<Integer, CompoundTag> entry : this.changedSlots(PlayerInventoryUtil.enderSlots(before), PlayerInventoryUtil.enderSlots(tag)).entrySet()) {
				commands.add(this.slotCommand(uuid, "enderchest." + entry.getKey(), entry.getValue()));
			}
			return this.run(commands, Set.of("commands.item.entity.set.success.single")).thenRun(() -> this.snapshots.put(key, tag.copy()));
		});
	}

	private Map<Integer, CompoundTag> changedSlots(Map<Integer, CompoundTag> previous, Map<Integer, CompoundTag> next) {
		Map<Integer, CompoundTag> changed = new LinkedHashMap<>();
		for (Map.Entry<Integer, CompoundTag> entry : next.entrySet()) {
			if (!entry.getValue().equals(previous.get(entry.getKey()))) {
				changed.put(entry.getKey(), entry.getValue());
			}
		}
		for (Integer slot : previous.keySet()) {
			if (!next.containsKey(slot)) {
				changed.put(slot, null);
			}
		}
		return changed;
	}

	private String slotCommand(UUID uuid, int slot, CompoundTag item) {
		String name = ItemCommandUtil.slotName(slot);
		if (name == null) {
			throw new TargetException(this.ctx.tr("error.slot"));
		}
		return this.slotCommand(uuid, name, item);
	}

	private String slotCommand(UUID uuid, String slot, CompoundTag item) {
		String selector = Objects.equals(uuid, this.ctx.client().player().uuid()) ? "@s" : uuid.toString();
		String command;
		try {
			command = ItemCommandUtil.replaceCommand(selector, slot, item, this.version(), this.dialect());
		} catch (IllegalArgumentException e) {
			throw new TargetException(this.ctx.tr("error." + e.getMessage()));
		}
		if (!DataCommandUtil.fits(command)) {
			throw new TargetException(this.ctx.tr("error.item_too_long"));
		}
		return command;
	}

	@Override
	public CompletableFuture<Void> setPlayerSlot(UUID uuid, int slot, CompoundTag item) {
		this.snapshots.remove("player:" + uuid);
		try {
			return this.run(List.of(this.slotCommand(uuid, slot, item)), Set.of("commands.item.entity.set.success.single"));
		} catch (TargetException e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	@Override
	public CompletableFuture<List<String>> storageIds() {
		return CompletableFuture.completedFuture(List.of());
	}

	@Override
	public CompletableFuture<CompoundTag> readStorage(String id) {
		return this.query("", "storage " + id, "storage:" + id);
	}

	@Override
	public CompletableFuture<Void> writeStorage(String id, CompoundTag tag) {
		return this.writeData("", "storage " + id, "storage:" + id, tag);
	}

	@Override
	public CompletableFuture<Map<String, String>> gameRules() {
		List<String> names = this.ctx.client().commands().serverChildren("gamerule");
		Map<String, String> rules = new ConcurrentHashMap<>();
		List<CompletableFuture<Text>> results = new ArrayList<>();
		for (String name : names) {
			results.add(this.queue.submit("gamerule " + name, Set.of("commands.gamerule.query"), NONE).handle((message, error) -> {
				TranslatableContent content = message == null ? null : TextQueryUtil.firstTranslatable(message);
				String value = content == null ? null : TextQueryUtil.argText(content, -1);
				if (value != null) {
					rules.put(name, value);
				}
				return message;
			}));
		}
		return CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).thenApply(done -> {
			if (rules.isEmpty() && !names.isEmpty()) {
				throw new TargetException(this.ctx.tr("error.timeout"));
			}
			Map<String, String> ordered = new LinkedHashMap<>();
			for (String name : names) {
				if (rules.containsKey(name)) {
					ordered.put(name, rules.get(name));
				}
			}
			return ordered;
		});
	}

	@Override
	public CompletableFuture<Void> setGameRule(String rule, String value) {
		String command = "gamerule " + rule + " " + value;
		if (!DataCommandUtil.fits(command) || !DataCommandUtil.allowedText(command)) {
			return CompletableFuture.failedFuture(new TargetException(this.ctx.tr("error.too_long")));
		}
		return this.queue.submit(command, Set.of("commands.gamerule.set"), RULE_UNCHANGED).thenApply(message -> null);
	}
}
