package com.nick22985.nbteditor.ui.tab;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record InventoryTab(Supplier<GameItem> icon, Text label, Predicate<TabScreen> shownOn, Runnable onClick) {
	public static final RegistryKey<InventoryTab> REGISTRY = RegistryKey.of("inventory_tab", InventoryTab.class);
}
