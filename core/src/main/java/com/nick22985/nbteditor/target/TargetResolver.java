package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.platform.Hit;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface TargetResolver {
	RegistryKey<TargetResolver> REGISTRY = RegistryKey.of("target_resolver", TargetResolver.class);

	Optional<Target> resolve(Hit hit);
}
