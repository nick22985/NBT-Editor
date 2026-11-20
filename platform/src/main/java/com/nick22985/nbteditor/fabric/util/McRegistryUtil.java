package com.nick22985.nbteditor.fabric.util;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McRegistryUtil {
	private McRegistryUtil() {
	}

	@SuppressWarnings("unchecked")
	public static Optional<Registry<Object>> registry(RegistryAccess access, String id) {
		Identifier key = McIdUtil.tryParse(id);
		if (key == null) {
			return Optional.empty();
		}
		ResourceKey<Registry<Object>> registryKey = ResourceKey.createRegistryKey(key);
		Optional<? extends Registry<Object>> found;
		//? if >=1.21.2 {
		found = access.lookup(registryKey);
		//?} else
		//found = access.registry(registryKey);
		if (found.isPresent()) {
			return Optional.of(found.get());
		}
		Object builtin = BuiltInRegistries.REGISTRY.getValue(key);
		return builtin instanceof Registry<?> registry ? Optional.of((Registry<Object>) registry) : Optional.empty();
	}

	public static String keyOf(Registry<Object> registry, Object value) {
		Identifier key = registry.getKey(value);
		return key == null ? null : key.toString();
	}
}
