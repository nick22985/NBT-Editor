package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.ExportData;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.DataFixService;
import com.nick22985.nbteditor.platform.DataKind;
import com.nick22985.nbteditor.target.SubjectKind;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ExportUtil {
	public static final String TYPE = "type";
	public static final String DATA_VERSION = "DataVersion";
	public static final String ID = "id";
	public static final String STATE = "state";
	public static final String TAG = "tag";
	public static final String ITEM = "item";
	public static final String BLOCK = "block";
	public static final String ENTITY = "entity";
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

	private ExportUtil() {
	}

	public static CompoundTag toFile(ExportData data) {
		CompoundTag out;
		if (data.kind() == SubjectKind.BLOCK) {
			out = new CompoundTag();
			out.putString(ID, data.tag().getString(BlockDocUtil.NAME, ""));
			CompoundTag state = data.tag().getCompound(BlockDocUtil.PROPERTIES);
			out.put(STATE, state == null ? new CompoundTag() : state.copy());
			CompoundTag blockEntity = data.tag().getCompound(BlockDocUtil.NBT);
			out.put(TAG, blockEntity == null ? new CompoundTag() : blockEntity.copy());
			out.putString(TYPE, BLOCK);
		} else if (data.kind() == SubjectKind.ENTITY) {
			out = new CompoundTag();
			CompoundTag tag = data.tag().copy();
			tag.remove(ID);
			out.putString(ID, data.tag().getString(ID, ""));
			out.put(TAG, tag);
			out.putString(TYPE, ENTITY);
		} else {
			out = data.tag().copy();
			out.putString(TYPE, ITEM);
		}
		out.putInt(DATA_VERSION, data.dataVersion());
		return out;
	}

	public static ExportData fromFile(CompoundTag file, int fallbackVersion) {
		int version = file.getInt(DATA_VERSION, fallbackVersion);
		String type = file.getString(TYPE, ITEM);
		String id = file.getString(ID, "");
		if (id.isEmpty()) {
			return null;
		}
		if (BLOCK.equals(type)) {
			CompoundTag doc = new CompoundTag();
			doc.putString(BlockDocUtil.NAME, id);
			CompoundTag state = file.getCompound(STATE);
			doc.put(BlockDocUtil.PROPERTIES, state == null ? new CompoundTag() : state.copy());
			CompoundTag blockEntity = file.getCompound(TAG);
			if (blockEntity != null && !blockEntity.isEmpty()) {
				doc.put(BlockDocUtil.NBT, blockEntity.copy());
			}
			return new ExportData(SubjectKind.BLOCK, doc, version);
		}
		if (ENTITY.equals(type)) {
			CompoundTag tag = file.getCompound(TAG);
			CompoundTag entity = new CompoundTag();
			entity.putString(ID, id);
			if (tag != null) {
				entity.merge(tag);
			}
			return new ExportData(SubjectKind.ENTITY, entity, version);
		}
		if (!ITEM.equals(type)) {
			return null;
		}
		CompoundTag item = file.copy();
		item.remove(TYPE);
		item.remove(DATA_VERSION);
		return new ExportData(SubjectKind.ITEM, item, version);
	}

	public static ExportData upgrade(ExportData data, DataFixService fixes) {
		int to = fixes.dataVersion();
		if (data.dataVersion() <= 0 || data.dataVersion() >= to) {
			return data;
		}
		if (data.kind() == SubjectKind.ENTITY) {
			return new ExportData(data.kind(), fixes.upgrade(DataKind.ENTITY, data.tag(), data.dataVersion()), to);
		}
		if (data.kind() != SubjectKind.BLOCK) {
			return new ExportData(data.kind(), fixes.upgrade(DataKind.ITEM_STACK, data.tag(), data.dataVersion()), to);
		}
		CompoundTag state = new CompoundTag();
		state.putString(BlockDocUtil.NAME, data.tag().getString(BlockDocUtil.NAME, ""));
		CompoundTag properties = data.tag().getCompound(BlockDocUtil.PROPERTIES);
		if (properties != null && !properties.isEmpty()) {
			state.put(BlockDocUtil.PROPERTIES, properties.copy());
		}
		CompoundTag doc = fixes.upgrade(DataKind.BLOCK_STATE, state, data.dataVersion());
		if (!doc.contains(BlockDocUtil.PROPERTIES)) {
			doc.put(BlockDocUtil.PROPERTIES, new CompoundTag());
		}
		CompoundTag blockEntity = data.tag().getCompound(BlockDocUtil.NBT);
		if (blockEntity != null && !blockEntity.isEmpty()) {
			doc.put(BlockDocUtil.NBT, blockEntity.contains(ID) ? fixes.upgrade(DataKind.BLOCK_ENTITY, blockEntity, data.dataVersion()) : blockEntity.copy());
		}
		return new ExportData(data.kind(), doc, to);
	}

	public static String fileName(String name, LocalDateTime time) {
		String base = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
		if (base.isEmpty()) {
			base = "export";
		}
		return base + "_" + STAMP.format(time);
	}

	public static String withExtension(String name, String extension) {
		return name.toLowerCase(Locale.ROOT).endsWith("." + extension) ? name : name + "." + extension;
	}

	public static String giveCommand(CompoundTag item, GameVersion version, SnbtDialect dialect) {
		int count = ItemStackUtil.count(item);
		return "give @p " + ItemCommandUtil.argument(item, version, dialect) + (count > 1 ? " " + count : "");
	}

	public static String getCommand(CompoundTag item, SnbtDialect dialect) {
		return "get snbt " + SnbtUtil.write(item, dialect);
	}

	public static String setblockCommand(BlockData block, SnbtDialect dialect) {
		StringBuilder out = new StringBuilder("setblock ~ ~ ~ ").append(block.stateString());
		CompoundTag blockEntity = block.blockEntity() == null ? null : block.blockEntity().copy();
		if (blockEntity != null) {
			for (String key : PickItemUtil.POSITION_KEYS) {
				blockEntity.remove(key);
			}
			blockEntity.remove(ID);
		}
		if (blockEntity != null && !blockEntity.isEmpty()) {
			out.append(SnbtUtil.write(blockEntity, dialect));
		}
		return out.toString();
	}

	public static String summonCommand(CompoundTag entity, SnbtDialect dialect) {
		CompoundTag data = PickItemUtil.summonable(entity);
		String id = ItemStackUtil.namespaced(data.getString(ID, ""));
		data.remove(ID);
		for (String key : PickItemUtil.PLACEMENT_KEYS) {
			data.remove(key);
		}
		return "summon " + id + " ~ ~ ~" + (data.isEmpty() ? "" : " " + SnbtUtil.write(data, dialect));
	}

	public static String command(ExportData data, GameVersion version, SnbtDialect dialect) {
		if (data.kind() == SubjectKind.BLOCK) {
			return setblockCommand(BlockDocUtil.fromTag(data.tag()), dialect);
		}
		if (data.kind() == SubjectKind.ENTITY) {
			return summonCommand(data.tag(), dialect);
		}
		return giveCommand(data.tag(), version, dialect);
	}

	public static CompoundTag commandBlock(String command, GameVersion version, SnbtDialect dialect) {
		CompoundTag item = ItemStackUtil.create("minecraft:command_block", 1, version);
		CompoundTag blockEntity = new CompoundTag();
		blockEntity.putString("Command", command.startsWith("/") ? command.substring(1) : command);
		BlockEntityUtil.write(item, true, version, "minecraft:command_block", blockEntity);
		return item;
	}
}
