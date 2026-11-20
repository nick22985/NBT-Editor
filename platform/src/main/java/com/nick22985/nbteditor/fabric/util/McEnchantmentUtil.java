package com.nick22985.nbteditor.fabric.util;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.platform.RegistryService;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
//? if >=1.21 {
import net.minecraft.tags.EnchantmentTags;
//?}
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McEnchantmentUtil {
	private McEnchantmentUtil() {
	}

	public static Enchantment enchantment(String id) {
		Identifier key = McIdUtil.tryParse(id);
		if (key == null) {
			return null;
		}
		Optional<Registry<Object>> registry = McRegistryUtil.registry(NbteRuntime.INSTANCE.registries().get(), RegistryService.ENCHANTMENT);
		if (registry.isEmpty() || !registry.get().containsKey(key)) {
			return null;
		}
		//? if >=1.21.2 {
		Object value = registry.get().getValue(key);
		//?} else
		//Object value = registry.get().get(key);
		return value instanceof Enchantment enchantment ? enchantment : null;
	}

	@SuppressWarnings("unchecked")
	public static boolean curse(Enchantment enchantment) {
		//? if >=1.21 {
		Optional<Registry<Object>> registry = McRegistryUtil.registry(NbteRuntime.INSTANCE.registries().get(), RegistryService.ENCHANTMENT);
		return registry.isPresent() && ((Registry<Enchantment>) (Registry<?>) registry.get()).wrapAsHolder(enchantment).is(EnchantmentTags.CURSE);
		//?} else
		//return enchantment.isCurse();
	}
}
