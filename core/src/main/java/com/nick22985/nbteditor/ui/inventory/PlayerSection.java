package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.text.Text;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PlayerSection extends SlotSection {
	public static final int HOTBAR = 9;
	public static final int MAIN = 27;
	private static final int HOTBAR_GAP = 4;

	private final PlayerService player;
	private final ItemTargets targets;

	public PlayerSection(Text label, PlayerService player, ItemTargets targets) {
		super(label, ContainerLayout.stack(HOTBAR_GAP, List.of(ContainerLayout.grid(HOTBAR, 3), ContainerLayout.grid(HOTBAR, 1))));
		this.player = player;
		this.targets = targets;
	}

	public int playerSlot(int index) {
		return index < MAIN ? index + HOTBAR : index - MAIN;
	}

	public int index(int playerSlot) {
		return playerSlot < HOTBAR ? playerSlot + MAIN : playerSlot - HOTBAR;
	}

	public int hotbarIndex(int hotbar) {
		return MAIN + hotbar;
	}

	public SlotSection lockPlayerSlot(int playerSlot) {
		if (playerSlot >= 0 && playerSlot < MAIN + HOTBAR) {
			this.lock(this.index(playerSlot));
		}
		return this;
	}

	@Override
	public boolean writable(int index) {
		return this.player.creative() && super.writable(index);
	}

	@Override
	public GameItem get(int index) {
		return this.player.slot(this.playerSlot(index));
	}

	@Override
	public void set(int index, GameItem item) {
		this.player.setSlot(this.playerSlot(index), item);
	}

	@Override
	public Target target(int index) {
		return this.targets == null ? null : this.targets.slot(this.playerSlot(index));
	}
}
