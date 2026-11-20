package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface GameItem {
	CompoundTag tag();

	String id();

	int count();

	boolean isEmpty();

	Text name();

	List<Text> tooltip(boolean advanced);

	List<String> errors();

	GameItem withCount(int count);
}
