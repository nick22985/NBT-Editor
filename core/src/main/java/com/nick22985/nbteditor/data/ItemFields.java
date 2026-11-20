package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemFields {
	public static final Field<Text> CUSTOM_NAME = new CustomNameField("custom_name", "Name");
	public static final Field<Text> ITEM_NAME = new ItemNameField();
	public static final Field<List<Text>> LORE = new LoreField();
	public static final Field<Map<String, Integer>> ENCHANTMENTS = new EnchantmentsField("enchantments", "Enchantments");
	public static final Field<Map<String, Integer>> STORED_ENCHANTMENTS = new EnchantmentsField("stored_enchantments", "StoredEnchantments");
	public static final Field<Boolean> UNBREAKABLE = new UnbreakableField();
	public static final Field<Integer> DAMAGE = new IntDataField("damage", "Damage");
	public static final Field<Integer> REPAIR_COST = new IntDataField("repair_cost", "RepairCost");
	public static final Field<Integer> CUSTOM_MODEL_DATA = new CustomModelDataField();
	public static final Field<List<AttributeModifier>> ATTRIBUTE_MODIFIERS = new AttributeModifiersField();
	public static final Field<Integer> DYED_COLOR = new DyedColorField();
	public static final Field<ArmorTrim> TRIM = new ArmorTrimField();
	public static final Field<PotionContents> POTION_CONTENTS = new PotionContentsField();
	public static final Field<List<PotionEffect>> STEW_EFFECTS = new StewEffectsField();
	public static final Field<BookContent> BOOK = new BookField();
	public static final Field<List<ContainerSlot>> CONTAINER = new ContainerField();
	public static final Field<List<CompoundTag>> BUNDLE = new BundleField();

	private ItemFields() {
	}
}
