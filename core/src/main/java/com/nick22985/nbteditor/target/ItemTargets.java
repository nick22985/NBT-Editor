package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.platform.Hand;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ItemTargets {
	Target held(Hand hand);

	Target slot(int slot);

	Target cursor();

	Target nested(Target parent, NbtPath path);
}
