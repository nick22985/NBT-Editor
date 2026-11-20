package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.text.Text;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface RegistryService {
	String ITEM = "minecraft:item";
	String BLOCK = "minecraft:block";
	String ENTITY_TYPE = "minecraft:entity_type";
	String ENCHANTMENT = "minecraft:enchantment";
	String MOB_EFFECT = "minecraft:mob_effect";
	String POTION = "minecraft:potion";
	String ATTRIBUTE = "minecraft:attribute";
	String BLOCK_ENTITY_TYPE = "minecraft:block_entity_type";
	String SOUND_EVENT = "minecraft:sound_event";
	String TRIM_MATERIAL = "minecraft:trim_material";
	String TRIM_PATTERN = "minecraft:trim_pattern";
	String DIMENSION = "minecraft:dimension";
	String DATA_COMPONENT_TYPE = "minecraft:data_component_type";

	List<String> ids(String registry);

	boolean contains(String registry, String id);

	Text name(String registry, String id);

	Map<String, List<String>> blockProperties(String blockId);

	Map<String, String> defaultBlockState(String blockId);

	Set<EntityTrait> entityTraits(String entityId);

	String blockItem(String blockId);

	EnchantmentInfo enchantment(String id);

	boolean canEnchant(String enchantment, GameItem item);
}
