package com.nick22985.nbteditor.fabric.slot;

import com.nick22985.nbteditor.fabric.util.McSlotUtil;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.Hooks;
import com.nick22985.nbteditor.hook.SlotAction;
import com.nick22985.nbteditor.hook.SlotCheckEvent;
import com.nick22985.nbteditor.hook.SlotRule;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotGuard {
	private static final int MAX_DEPTH = 8;

	private final Hooks hooks;
	private final ThreadLocal<Deque<SlotClick>> clicks = ThreadLocal.withInitial(ArrayDeque::new);

	public SlotGuard(Hooks hooks) {
		this.hooks = hooks;
	}

	public void begin(AbstractContainerMenu menu, Player player) {
		Deque<SlotClick> stack = this.clicks.get();
		if (stack.size() >= MAX_DEPTH) {
			stack.clear();
		}
		stack.push(new SlotClick(menu, player));
	}

	public void end() {
		Deque<SlotClick> stack = this.clicks.get();
		if (!stack.isEmpty()) {
			stack.pop();
		}
	}

	public boolean mayPlace(Slot slot, boolean vanilla) {
		return vanilla || this.allowed(slot, SlotAction.PLACE);
	}

	public boolean mayPickup(Slot slot, boolean vanilla) {
		return vanilla || this.allowed(slot, SlotAction.PICKUP);
	}

	public boolean bundle(boolean vanilla) {
		if (vanilla) {
			return true;
		}
		SlotClick click = this.clicks.get().peek();
		return click != null && this.check(SlotRule.BUNDLE, SlotAction.PLACE, click.player());
	}

	private boolean allowed(Slot slot, SlotAction action) {
		SlotClick click = this.clicks.get().peek();
		if (click == null || !this.hooks.has(CoreHooks.SLOT_CHECK)) {
			return false;
		}
		SlotRule rule = McSlotUtil.rule(click.menu(), slot);
		return rule != null && this.check(rule, action, click.player());
	}

	private boolean check(SlotRule rule, SlotAction action, Player player) {
		if (player == null || !this.hooks.has(CoreHooks.SLOT_CHECK)) {
			return false;
		}
		return this.hooks.fire(CoreHooks.SLOT_CHECK, new SlotCheckEvent(rule, action, player.getUUID(), player instanceof ServerPlayer)).allowed();
	}
}
