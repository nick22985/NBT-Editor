package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.fabric.util.McItemUtil;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.ItemService;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricItemService implements ItemService {
	@Override
	public GameItem fromTag(CompoundTag tag) {
		return FabricGameItem.from(tag);
	}

	@Override
	public GameItem empty() {
		return FabricGameItem.of(ItemStack.EMPTY);
	}

	@Override
	public GameItem of(String id, int count) {
		Identifier key = McIdUtil.tryParse(id);
		if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
			return this.empty();
		}
		return FabricGameItem.of(new ItemStack(BuiltInRegistries.ITEM.getValue(key), count));
	}

	@Override
	public boolean isItem(String id) {
		Identifier key = McIdUtil.tryParse(id);
		return key != null && BuiltInRegistries.ITEM.containsKey(key) && BuiltInRegistries.ITEM.getValue(key) != Items.AIR;
	}

	@Override
	public int maxStackSize(GameItem item) {
		return FabricGameItem.stackOf(item).getMaxStackSize();
	}

	@Override
	public int dataVersion() {
		return McItemUtil.dataVersion();
	}

	@Override
	public CompoundTag upgrade(CompoundTag tag, int fromDataVersion) {
		return McItemUtil.upgrade(tag, fromDataVersion);
	}
}
