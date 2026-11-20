package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface WorldService {
	boolean present();

	String dimension();

	Hit targeted(double reach, boolean includeFluids);

	BlockData clientBlock(BlockPos pos);

	CompoundTag clientEntity(int networkId);

	boolean isSingleplayer();

	String serverAddress();

	List<GameItem> nearbyPlayerItems(double range);

	void pickListener(PickListener listener);
}
