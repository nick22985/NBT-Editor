package com.nick22985.nbteditor.fabric.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.nbt.CompoundTag;
//? if >=1.21.6 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McBlockUtil {
	private McBlockUtil() {
	}

	public static net.minecraft.core.BlockPos toMc(com.nick22985.nbteditor.platform.BlockPos pos) {
		return new net.minecraft.core.BlockPos(pos.x(), pos.y(), pos.z());
	}

	public static com.nick22985.nbteditor.platform.BlockPos fromMc(net.minecraft.core.BlockPos pos) {
		return new com.nick22985.nbteditor.platform.BlockPos(pos.getX(), pos.getY(), pos.getZ());
	}

	public static Block block(String id) {
		Identifier key = McIdUtil.tryParse(id);
		if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
			return null;
		}
		return BuiltInRegistries.BLOCK.getValue(key);
	}

	public static String id(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	public static String id(BlockState state) {
		return id(state.getBlock());
	}

	public static Map<String, String> properties(BlockState state) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Property<?> property : state.getProperties()) {
			out.put(property.getName(), valueName(state, property));
		}
		return out;
	}

	public static Map<String, List<String>> propertyValues(Block block) {
		Map<String, List<String>> out = new LinkedHashMap<>();
		for (Property<?> property : block.getStateDefinition().getProperties()) {
			out.put(property.getName(), possibleValues(property));
		}
		return out;
	}

	public static BlockState state(String id, Map<String, String> properties) {
		Block block = block(id);
		if (block == null) {
			return Blocks.AIR.defaultBlockState();
		}
		BlockState state = block.defaultBlockState();
		if (properties == null) {
			return state;
		}
		for (Property<?> property : block.getStateDefinition().getProperties()) {
			String value = properties.get(property.getName());
			if (value != null) {
				state = withValue(state, property, value);
			}
		}
		return state;
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}

	private static <T extends Comparable<T>> List<String> possibleValues(Property<T> property) {
		List<String> values = new ArrayList<>();
		for (T value : property.getPossibleValues()) {
			values.add(property.getName(value));
		}
		return values;
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
		Optional<T> parsed = property.getValue(value);
		return parsed.isPresent() ? state.setValue(property, parsed.get()) : state;
	}

	public static CompoundTag saveBlockEntity(BlockEntity blockEntity) {
		//? if >=1.20.5 {
		return blockEntity.saveWithoutMetadata(NbteRuntime.INSTANCE.registries().get());
		//?} else if >=1.18 {
		/*return blockEntity.saveWithoutMetadata();
		*///?} else {
		/*CompoundTag tag = blockEntity.save(new CompoundTag());
		tag.remove("id");
		tag.remove("x");
		tag.remove("y");
		tag.remove("z");
		return tag;
		*///?}
	}

	public static void loadBlockEntity(BlockEntity blockEntity, CompoundTag tag) {
		//? if >=1.21.6 {
		blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, NbteRuntime.INSTANCE.registries().get(), tag));
		//?} else if >=1.20.5 {
		/*blockEntity.loadWithComponents(tag, NbteRuntime.INSTANCE.registries().get());
		*///?} else
		//blockEntity.load(McBlockUtil.withValidCustomName(tag));
	}

	//? if <1.20.5 {
	/*private static CompoundTag withValidCustomName(CompoundTag tag) {
		if (!tag.contains("CustomName", 8)) {
			return tag;
		}
		String raw = tag.getString("CustomName");
		try {
			net.minecraft.network.chat.Component.Serializer.fromJson(raw);
			return tag;
		} catch (RuntimeException e) {
			CompoundTag copy = tag.copy();
			copy.putString("CustomName", new com.google.gson.JsonPrimitive(raw).toString());
			return copy;
		}
	}
	*///?}
}
