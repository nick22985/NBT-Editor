package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.item.DecodedItem;
import com.nick22985.nbteditor.platform.DataKind;
import com.nick22985.nbteditor.util.ItemStackUtil;
//? if >=1.20.5 {
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
//?}
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McItemUtil {
	private McItemUtil() {
	}

	public static int dataVersion() {
		return McDataFixUtil.dataVersion();
	}

	public static com.nick22985.nbteditor.nbt.CompoundTag upgrade(com.nick22985.nbteditor.nbt.CompoundTag tag, int fromDataVersion) {
		return McDataFixUtil.upgrade(DataKind.ITEM_STACK, tag, fromDataVersion);
	}

	public static DecodedItem decode(com.nick22985.nbteditor.nbt.CompoundTag tag) {
		List<String> errors = new ArrayList<>();
		if (ItemStackUtil.isEmpty(tag)) {
			return new DecodedItem(ItemStack.EMPTY, errors);
		}
		CompoundTag mc = McTagUtil.toMc(tag);
		//? if >=1.20.5 {
		DynamicOps<Tag> ops = NbteRuntime.INSTANCE.registries().get().createSerializationContext(NbtOps.INSTANCE);
		DataResult<ItemStack> full = ItemStack.OPTIONAL_CODEC.parse(ops, mc);
		if (full.result().isPresent()) {
			return new DecodedItem(full.result().get(), errors);
		}
		if (!(mc.get("components") instanceof CompoundTag components)) {
			errors.add(message(full));
			return new DecodedItem(ItemStack.EMPTY, errors);
		}
		CompoundTag base = mc.copy();
		base.remove("components");
		DataResult<ItemStack> baseResult = ItemStack.OPTIONAL_CODEC.parse(ops, base);
		if (baseResult.result().isEmpty()) {
			errors.add(message(baseResult));
			return new DecodedItem(ItemStack.EMPTY, errors);
		}
		ItemStack best = baseResult.result().get();
		CompoundTag kept = new CompoundTag();
		for (String key : tag.getCompound("components").keys()) {
			CompoundTag trial = kept.copy();
			trial.put(key, components.get(key));
			CompoundTag attempt = base.copy();
			attempt.put("components", trial);
			DataResult<ItemStack> result = ItemStack.OPTIONAL_CODEC.parse(ops, attempt);
			if (result.result().isPresent()) {
				kept = trial;
				best = result.result().get();
			} else {
				errors.add(key + ": " + message(result));
			}
		}
		return new DecodedItem(best, errors);
		//?} else {
		/*ItemStack stack = ItemStack.of(mc);
		if (stack.isEmpty()) {
			errors.add("Unknown item " + ItemStackUtil.id(tag));
		}
		return new DecodedItem(stack, errors);
		*///?}
	}

	public static com.nick22985.nbteditor.nbt.CompoundTag encode(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return ItemStackUtil.create(ItemStackUtil.AIR, 0, McVersionUtil.current());
		}
		//? if >=1.20.5 {
		DynamicOps<Tag> ops = NbteRuntime.INSTANCE.registries().get().createSerializationContext(NbtOps.INSTANCE);
		Tag encoded = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
		return encoded instanceof CompoundTag compound ? McTagUtil.fromMc(compound) : ItemStackUtil.create(ItemStackUtil.AIR, 0, McVersionUtil.current());
		//?} else
		//return McTagUtil.fromMc(stack.save(new CompoundTag()));
	}

	//? if >=1.20.5 {
	private static String message(DataResult<?> result) {
		return result.error().map(error -> error.message()).orElse("Invalid item");
	}
	//?}
}
