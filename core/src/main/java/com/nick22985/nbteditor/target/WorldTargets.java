package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.Hit;
import java.util.Optional;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface WorldTargets {
	Target block(String dimension, BlockPos pos);

	Target entity(String dimension, UUID uuid, int networkId);

	Target self();

	Target player(UUID uuid, String name);

	Target storage(String id);

	Target gameRules();

	Optional<Target> hit(Hit hit);
}
