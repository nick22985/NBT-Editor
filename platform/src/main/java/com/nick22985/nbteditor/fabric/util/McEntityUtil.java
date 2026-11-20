package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.UuidUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
//? if >=1.21.6 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
//?}
import net.minecraft.world.entity.Entity;
//? if >=1.21.2 {
import net.minecraft.world.entity.EntitySpawnReason;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McEntityUtil {
	private McEntityUtil() {
	}

	public static String typeId(Entity entity) {
		return EntityType.getKey(entity.getType()).toString();
	}

	public static CompoundTag save(Entity entity) {
		//? if >=1.21.6 {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, NbteRuntime.INSTANCE.registries().get());
		entity.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		//?} else
		//CompoundTag tag = entity.saveWithoutId(new CompoundTag());
		tag.putString("id", typeId(entity));
		return tag;
	}

	public static void load(Entity entity, CompoundTag tag) {
		CompoundTag copy = tag.copy();
		copy.remove("id");
		//? if >=1.21.6 {
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, NbteRuntime.INSTANCE.registries().get(), copy));
		//?} else
		//entity.load(copy);
	}

	public static com.nick22985.nbteditor.nbt.CompoundTag saveTree(Entity entity) {
		com.nick22985.nbteditor.nbt.CompoundTag tag = McTagUtil.fromMc(save(entity));
		List<Entity> passengers = entity.getPassengers();
		if (!passengers.isEmpty()) {
			com.nick22985.nbteditor.nbt.ListTag list = new com.nick22985.nbteditor.nbt.ListTag();
			for (Entity passenger : passengers) {
				list.add(saveTree(passenger));
			}
			tag.put("Passengers", list);
		}
		return tag;
	}

	public static boolean sameTree(Entity entity, com.nick22985.nbteditor.nbt.CompoundTag tag) {
		String id = tag.getString("id", typeId(entity));
		if (!ItemStackUtil.namespaced(id).equals(typeId(entity))) {
			return false;
		}
		List<Entity> passengers = entity.getPassengers();
		com.nick22985.nbteditor.nbt.ListTag list = tag.getList("Passengers");
		int size = list == null ? 0 : list.size();
		if (size != passengers.size()) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!(list.get(i) instanceof com.nick22985.nbteditor.nbt.CompoundTag)) {
				return false;
			}
			com.nick22985.nbteditor.nbt.CompoundTag child = (com.nick22985.nbteditor.nbt.CompoundTag) list.get(i);
			Entity passenger = passengers.get(i);
			if (!passenger.getUUID().equals(UuidUtil.fromTag(child.get("UUID"))) || !sameTree(passenger, child)) {
				return false;
			}
		}
		return true;
	}

	public static void loadTree(Entity entity, com.nick22985.nbteditor.nbt.CompoundTag tag) {
		UUID uuid = entity.getUUID();
		com.nick22985.nbteditor.nbt.CompoundTag copy = tag.copy();
		com.nick22985.nbteditor.nbt.ListTag list = copy.getList("Passengers");
		copy.remove("Passengers");
		load(entity, McTagUtil.toMc(copy));
		entity.setUUID(uuid);
		List<Entity> passengers = entity.getPassengers();
		for (int i = 0; list != null && i < list.size() && i < passengers.size(); i++) {
			loadTree(passengers.get(i), (com.nick22985.nbteditor.nbt.CompoundTag) list.get(i));
		}
	}

	public static Level level(Entity entity) {
		//? if >=1.20 {
		return entity.level();
		//?} else
		//return entity.level;
	}

	public static EntityType<?> type(String id) {
		Identifier key = McIdUtil.tryParse(id);
		if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
			return null;
		}
		return BuiltInRegistries.ENTITY_TYPE.getValue(key);
	}

	public static Entity create(EntityType<?> type, Level level) {
		//? if >=1.21.2 {
		return type.create(level, EntitySpawnReason.COMMAND);
		//?} else
		//return type.create(level);
	}

	public static void ride(Entity passenger, Entity vehicle) {
		//? if >=1.21.10 {
		passenger.startRiding(vehicle, true, false);
		//?} else
		//passenger.startRiding(vehicle, true);
	}

	public static void discardTree(Entity entity) {
		for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
			discardTree(passenger);
		}
		entity.discard();
	}

	public static Entity spawnTree(ServerLevel level, com.nick22985.nbteditor.nbt.CompoundTag tag) {
		EntityType<?> type = type(tag.getString("id", ""));
		if (type == null) {
			return null;
		}
		Entity entity = create(type, level);
		if (entity == null) {
			return null;
		}
		com.nick22985.nbteditor.nbt.CompoundTag copy = tag.copy();
		com.nick22985.nbteditor.nbt.ListTag passengers = copy.getList("Passengers");
		copy.remove("Passengers");
		load(entity, McTagUtil.toMc(copy));
		float yaw = entity.getYRot();
		float pitch = entity.getXRot();
		if (!level.addFreshEntity(entity)) {
			return null;
		}
		if (passengers != null) {
			for (com.nick22985.nbteditor.nbt.Tag child : passengers) {
				if (child instanceof com.nick22985.nbteditor.nbt.CompoundTag) {
					Entity passenger = spawnTree(level, (com.nick22985.nbteditor.nbt.CompoundTag) child);
					if (passenger != null) {
						float passengerYaw = passenger.getYRot();
						float passengerPitch = passenger.getXRot();
						ride(passenger, entity);
						restoreRotation(passenger, passengerYaw, passengerPitch);
					}
				}
			}
		}
		restoreRotation(entity, yaw, pitch);
		return entity;
	}

	public static void restoreRotation(Entity entity, float yaw, float pitch) {
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.setYHeadRot(yaw);
		entity.yRotO = yaw;
		entity.xRotO = pitch;
	}
}
