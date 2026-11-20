package com.nick22985.nbteditor.modules.enchantments;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.data.Field;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.ItemLostEvent;
import com.nick22985.nbteditor.hook.LostItemReason;
import com.nick22985.nbteditor.hook.SlotClickEvent;
import com.nick22985.nbteditor.hook.TooltipEvent;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookApplier {
	private final ModuleContext ctx;
	private final Setting<Boolean> enabled;

	public BookApplier(ModuleContext ctx, Setting<Boolean> enabled) {
		this.ctx = ctx;
		this.enabled = enabled;
	}

	public void register() {
		this.ctx.hooks().on(CoreHooks.SLOT_CLICK, this::click);
		this.ctx.hooks().on(CoreHooks.ITEM_TOOLTIP, this::tooltip);
	}

	private void click(SlotClickEvent event) {
		if (!this.enabled.get() || !event.modifiers().onlyPrimary() || !InventoryUtil.present(event.cursor())) {
			return;
		}
		GameItem cursor = event.cursor();
		GameItem clicked = event.slot().item();
		boolean cursorBook = EnchantmentsPane.BOOK.equals(cursor.id());
		if (!cursorBook && !EnchantmentsPane.BOOK.equals(clicked.id())) {
			return;
		}
		Optional<ItemTargets> targets = this.ctx.services().find(ItemTargets.class);
		Target slot = targets.isEmpty() || event.slot().playerSlot() == HoveredItem.NO_SLOT ? null : targets.get().slot(event.slot().playerSlot());
		if (slot == null || !slot.can(TargetCapability.WRITE) || !targets.get().cursor().can(TargetCapability.WRITE)) {
			return;
		}
		GameItem book = cursorBook ? cursor : clicked;
		GameItem item = cursorBook ? clicked : cursor;
		GameVersion version = CommandUtil.version(this.ctx);
		Map<String, Integer> stored = ItemFields.STORED_ENCHANTMENTS.get(book.tag(), version);
		if (stored == null || stored.isEmpty()) {
			return;
		}
		Field<Map<String, Integer>> field = EnchantmentsPane.BOOK.equals(item.id()) ? ItemFields.STORED_ENCHANTMENTS : ItemFields.ENCHANTMENTS;
		CompoundTag result = item.tag().copy();
		Map<String, Integer> current = field.get(result, version);
		Map<String, Integer> merged = current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
		merged.putAll(stored);
		field.write(result, version, merged);
		event.cancel();
		Target cursorTarget = targets.get().cursor();
		slot.save(result).thenCompose(done -> cursorTarget.save(this.ctx.client().items().empty().tag())).whenComplete((done, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null) {
				this.ctx.client().screens().toast(this.ctx.tr("tab"), ErrorUtil.message(error));
			} else {
				this.ctx.hooks().fire(CoreHooks.ITEM_LOST, new ItemLostEvent(book, LostItemReason.DELETED));
			}
		}));
	}

	private void tooltip(TooltipEvent event) {
		if (!this.enabled.get() || !EnchantmentsPane.BOOK.equals(event.item().id())) {
			return;
		}
		HoveredItem hovered = this.ctx.client().screens().hovered();
		if (hovered == null || hovered.playerSlot() == HoveredItem.NO_SLOT || this.ctx.client().screens().current() != null) {
			return;
		}
		Optional<ItemTargets> targets = this.ctx.services().find(ItemTargets.class);
		if (targets.isPresent() && targets.get().cursor().can(TargetCapability.WRITE)) {
			event.lines().add(this.ctx.tr("hint.apply").withStyle(Style.EMPTY.withColor(Formatting.GOLD)));
		}
	}
}
