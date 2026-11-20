package com.nick22985.nbteditor.fabric.item;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record DecodedItem(ItemStack stack, List<String> errors) {
}
