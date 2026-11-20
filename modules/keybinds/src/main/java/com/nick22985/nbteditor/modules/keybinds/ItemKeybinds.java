package com.nick22985.nbteditor.modules.keybinds;

import com.nick22985.nbteditor.command.CommandException;
import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.container.ItemStash;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.hook.ClientTickEvent;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.ItemLostEvent;
import com.nick22985.nbteditor.hook.LostItemReason;
import com.nick22985.nbteditor.hook.ScreenKeyEvent;
import com.nick22985.nbteditor.hook.TooltipEvent;
import com.nick22985.nbteditor.keybind.Keybind;
import com.nick22985.nbteditor.keybind.KeybindSpec;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemKeybinds {
	private final ModuleContext ctx;
	private final Setting<Boolean> inventoryKeys;
	private final Setting<Boolean> keyHints;
	private Keybind open;
	private Keybind copy;
	private Keybind stash;

	public ItemKeybinds(ModuleContext ctx, Setting<Boolean> inventoryKeys, Setting<Boolean> keyHints) {
		this.ctx = ctx;
		this.inventoryKeys = inventoryKeys;
		this.keyHints = keyHints;
	}

	public void register() {
		this.open = this.ctx.client().keybinds().register(KeybindSpec.of("open_editor", Key.N));
		this.copy = this.ctx.client().keybinds().register(KeybindSpec.of("copy_nbt", KeybindSpec.UNBOUND));
		this.stash = this.ctx.client().keybinds().register(KeybindSpec.of("client_chest", KeybindSpec.UNBOUND));
		this.ctx.hooks().on(CoreHooks.CLIENT_TICK, this::tick);
		this.ctx.hooks().on(CoreHooks.SCREEN_KEY, this::screenKey);
		this.ctx.hooks().on(CoreHooks.ITEM_TOOLTIP, this::tooltip);
	}

	private void tick(ClientTickEvent event) {
		boolean opened = false;
		while (this.open.consumeClick()) {
			if (event.inWorld() && !opened) {
				opened = true;
				this.openInWorld();
			}
		}
		while (this.copy.consumeClick()) {
			if (event.inWorld()) {
				this.copyHeld();
			}
		}
		while (this.stash.consumeClick()) {
			if (event.inWorld()) {
				this.stashHeld();
			}
		}
	}

	private void openInWorld() {
		try {
			CommandUtil.openEditor(this.ctx, CommandUtil.heldOrTargeted(this.ctx), null);
		} catch (CommandException e) {
			this.notify(e.text());
		}
	}

	private void copyHeld() {
		HoveredItem held = this.held();
		if (held != null) {
			this.copy(held);
		}
	}

	private void stashHeld() {
		HoveredItem held = this.held();
		if (held != null) {
			this.stash(held);
		}
	}

	private HoveredItem held() {
		int slot = this.ctx.client().player().selectedSlot();
		HoveredItem held = new HoveredItem(this.ctx.client().player().slot(slot), null, slot);
		if (!InventoryUtil.present(held.item())) {
			this.notify(CommandUtil.error("no_item"));
			return null;
		}
		return held;
	}

	private void screenKey(ScreenKeyEvent event) {
		if (event.typing() || event.hovered() == null || !InventoryUtil.present(event.hovered().item())) {
			return;
		}
		if (this.open.matches(event.input())) {
			if (this.openHovered(event.hovered())) {
				event.cancel();
			}
		} else if (this.copy.matches(event.input())) {
			this.copy(event.hovered());
			event.cancel();
		} else if (this.stash.matches(event.input())) {
			this.stash(event.hovered());
			event.cancel();
		} else if (!event.modScreen() && this.inventoryKeys.get() && this.inventoryKey(event.input(), event.hovered())) {
			event.cancel();
		}
	}

	private boolean inventoryKey(KeyInput input, HoveredItem hovered) {
		if (input.plain(Key.DELETE)) {
			return this.delete(hovered);
		}
		Modifiers modifiers = input.modifiers();
		if (!input.is(Key.SPACE) || modifiers.alt()) {
			return false;
		}
		Target target = this.target(hovered);
		if (target == null) {
			return false;
		}
		if (modifiers.primary()) {
			Optional<ContainerService> containers = this.ctx.services().find(ContainerService.class);
			if (containers.isEmpty() || !this.container(containers.get(), hovered.item())) {
				return false;
			}
			this.ctx.platform().scheduler().client(() -> containers.get().open(target));
			return true;
		}
		Optional<EditorService> editor = this.ctx.services().find(EditorService.class);
		if (editor.isEmpty()) {
			return false;
		}
		String tab = modifiers.shift() ? EditorService.FACTORY_TAB : null;
		this.ctx.platform().scheduler().client(() -> editor.get().open(target, tab));
		return true;
	}

	private boolean container(ContainerService containers, GameItem item) {
		return containers.format(SubjectKind.ITEM, item.tag(), CommandUtil.version(this.ctx)).isPresent();
	}

	private boolean delete(HoveredItem hovered) {
		Target target = this.target(hovered);
		if (target == null || !target.can(TargetCapability.WRITE)) {
			return false;
		}
		GameItem item = hovered.item();
		target.save(this.ctx.client().items().empty().tag()).whenComplete((done, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null) {
				this.notify(ErrorUtil.message(error));
			} else {
				this.ctx.hooks().fire(CoreHooks.ITEM_LOST, new ItemLostEvent(item, LostItemReason.DELETED));
			}
		}));
		return true;
	}

	private void tooltip(TooltipEvent event) {
		if (!this.keyHints.get() || !this.ctx.client().screens().anyOpen()) {
			return;
		}
		HoveredItem hovered = this.ctx.client().screens().hovered();
		if (hovered == null || hovered.item().isEmpty()) {
			return;
		}
		boolean modScreen = this.ctx.client().screens().current() != null;
		if (!modScreen && !this.inventoryKeys.get()) {
			return;
		}
		Target target = this.target(hovered);
		if (target == null) {
			return;
		}
		event.lines().add(this.hint("hint.edit"));
		event.lines().add(this.hint("hint.factory"));
		Optional<ContainerService> containers = this.ctx.services().find(ContainerService.class);
		if (containers.isPresent() && this.container(containers.get(), event.item())) {
			event.lines().add(this.hint("hint.container"));
		}
		if (target.can(TargetCapability.WRITE)) {
			event.lines().add(this.hint("hint.delete"));
		}
	}

	private Text hint(String key) {
		return this.ctx.tr(key).withStyle(Style.EMPTY.withColor(Formatting.GOLD));
	}

	private boolean openHovered(HoveredItem hovered) {
		Optional<EditorService> editor = this.ctx.services().find(EditorService.class);
		if (editor.isEmpty()) {
			return false;
		}
		Target target = this.target(hovered);
		if (target == null) {
			this.notify(this.ctx.tr("not_editable"));
		} else {
			this.ctx.platform().scheduler().client(() -> editor.get().open(target));
		}
		return true;
	}

	private Target target(HoveredItem hovered) {
		if (hovered.target() != null) {
			return hovered.target();
		}
		if (hovered.playerSlot() == HoveredItem.NO_SLOT) {
			return null;
		}
		return this.ctx.services().find(ItemTargets.class).map(targets -> targets.slot(hovered.playerSlot())).orElse(null);
	}

	private void copy(HoveredItem hovered) {
		this.ctx.client().system().setClipboard(hovered.item().tag().toSnbt());
		this.notify(this.ctx.tr("copied"));
	}

	private void stash(HoveredItem hovered) {
		Optional<ItemStash> stash = this.ctx.services().find(ItemStash.class);
		if (stash.isEmpty()) {
			this.notify(this.ctx.tr("no_chest"));
			return;
		}
		stash.get().insert(hovered.item()).whenComplete((page, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null || page == null || page < 0) {
				this.notify(this.ctx.tr("chest_full"));
			} else {
				this.notify(this.ctx.tr("stashed", page + 1));
			}
		}));
	}

	private void notify(Text message) {
		if (this.ctx.client().screens().anyOpen()) {
			this.ctx.client().screens().toast(this.ctx.tr("title"), message);
		} else {
			this.ctx.client().chat().actionBar(message);
		}
	}
}
