package com.nick22985.nbteditor.fabric.util;

//? if >=1.21.4 {
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.PickListener;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.6 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
//?} else
//import net.minecraft.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McPickUtil {
	private static final int MAIN_SLOTS = 36;
	private static final int HOTBAR_SLOTS = 9;
	private static final int HOTBAR_MENU_START = 36;

	private McPickUtil() {
	}

	public static boolean pick(BlockPos pos, boolean includeData) {
		PickListener listener = NbteRuntime.INSTANCE.pickListener();
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (listener == null || level == null || player == null || mc.gameMode == null || !player.hasInfiniteMaterials()) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		if (!listener.enabled()) {
			if (listener.debug()) {
				listener.picked(blockData(state, blockEntity), null, includeData);
			}
			return false;
		}
		if (state.isAir()) {
			return false;
		}
		ItemStack stack = state.getCloneItemStack(level, pos, includeData);
		if (stack.isEmpty() || !stack.isItemEnabled(level.enabledFeatures())) {
			return false;
		}
		if (blockEntity != null) {
			if (includeData) {
				addBlockData(stack, blockEntity, level.registryAccess());
			} else {
				addLook(stack, blockEntity.collectComponents());
			}
			Component name = stack.get(DataComponents.CUSTOM_NAME);
			if (name != null && name.getString().isBlank()) {
				stack.remove(DataComponents.CUSTOM_NAME);
			}
			stack = FabricGameItem.stackOf(listener.finish(FabricGameItem.of(stack)));
		}
		give(mc, player, stack);
		if (listener.debug()) {
			listener.picked(blockData(state, blockEntity), FabricGameItem.of(stack), includeData);
		}
		return true;
	}

	private static BlockData blockData(BlockState state, BlockEntity blockEntity) {
		return new BlockData(McBlockUtil.id(state), McBlockUtil.properties(state), blockEntity == null ? null : McTagUtil.fromMc(McBlockUtil.saveBlockEntity(blockEntity)));
	}

	private static void addBlockData(ItemStack stack, BlockEntity blockEntity, HolderLookup.Provider registries) {
		//? if >=1.21.6 {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		blockEntity.saveCustomOnly(output);
		blockEntity.removeComponentsFromTag(output);
		BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
		//?} else {
		/*CompoundTag tag = blockEntity.saveCustomOnly(registries);
		blockEntity.removeComponentsFromTag(tag);
		BlockItem.setBlockEntityData(stack, blockEntity.getType(), tag);
		*///?}
		stack.applyComponents(blockEntity.collectComponents());
	}

	private static void addLook(ItemStack stack, DataComponentMap components) {
		copy(stack, components, DataComponents.PROFILE);
		copy(stack, components, DataComponents.NOTE_BLOCK_SOUND);
		copy(stack, components, DataComponents.BANNER_PATTERNS);
		copy(stack, components, DataComponents.POT_DECORATIONS);
		copy(stack, components, DataComponents.CUSTOM_NAME);
	}

	private static <T> void copy(ItemStack stack, DataComponentMap components, DataComponentType<T> type) {
		T value = components.get(type);
		if (value != null) {
			stack.set(type, value);
		}
	}

	private static void give(Minecraft mc, LocalPlayer player, ItemStack stack) {
		Inventory inventory = player.getInventory();
		List<ItemStack> before = new ArrayList<>();
		for (int slot = 0; slot < MAIN_SLOTS; slot++) {
			before.add(inventory.getItem(slot).copy());
		}
		int found = inventory.findSlotMatchingItem(stack);
		if (Inventory.isHotbarSlot(found)) {
			//? if >=1.21.5 {
			inventory.setSelectedSlot(found);
			//?} else
			//inventory.selected = found;
		} else if (found != -1) {
			inventory.pickSlot(found);
		} else {
			inventory.addAndPickItem(stack);
		}
		for (int slot = 0; slot < MAIN_SLOTS; slot++) {
			ItemStack now = inventory.getItem(slot);
			if (!ItemStack.matches(before.get(slot), now)) {
				mc.gameMode.handleCreativeModeItemAdd(now, slot < HOTBAR_SLOTS ? HOTBAR_MENU_START + slot : slot);
			}
		}
	}
}
//?}
