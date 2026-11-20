package com.nick22985.nbteditor.platform;

import java.util.List;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface PlayerService {
	int OFFHAND_SLOT = 40;
	int HEAD_SLOT = 39;
	int CHEST_SLOT = 38;
	int LEGS_SLOT = 37;
	int FEET_SLOT = 36;

	boolean present();

	String name();

	UUID uuid();

	int networkId();

	Position position();

	boolean creative();

	int permissionLevel();

	int selectedSlot();

	GameItem slot(int slot);

	GameItem held(Hand hand);

	int heldSlot(Hand hand);

	boolean setSlot(int slot, GameItem item);

	GameItem cursor();

	void setCursor(GameItem item);

	boolean give(GameItem item);

	int inventoryChanges();

	List<GameItem> menuItems();
}
