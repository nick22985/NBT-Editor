package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.FabricScheduler;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.fabric.ui.McChatWarning;
import com.nick22985.nbteditor.fabric.ui.McTabBar;
import com.nick22985.nbteditor.fabric.util.McInputUtil;
import com.nick22985.nbteditor.fabric.util.McScreenUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.hook.ChatReceiveEvent;
import com.nick22985.nbteditor.hook.ClientTickEvent;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.DropScreen;
import com.nick22985.nbteditor.hook.FilesDropEvent;
import com.nick22985.nbteditor.hook.Hooks;
import com.nick22985.nbteditor.hook.ScreenKeyEvent;
import com.nick22985.nbteditor.hook.SlotClickEvent;
import com.nick22985.nbteditor.hook.TooltipEvent;
import com.nick22985.nbteditor.hook.WorldJoinEvent;
import com.nick22985.nbteditor.hook.WorldLeaveEvent;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.net.NetService;
import com.nick22985.nbteditor.platform.WorldService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.tab.InventoryTab;
import com.nick22985.nbteditor.ui.tab.InventoryTabs;
import com.nick22985.nbteditor.ui.tab.TabScreen;
import com.nick22985.nbteditor.ui.tab.TabScreenKind;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//? if >=1.19.3
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricClientHooks {
	private final Supplier<Hooks> hooks;
	private final Supplier<Kernel> kernel;
	private final Supplier<NetService> net;
	private final FabricScheduler scheduler;
	private final WorldService world;

	public FabricClientHooks(Supplier<Hooks> hooks, Supplier<Kernel> kernel, Supplier<NetService> net, FabricScheduler scheduler, WorldService world) {
		this.hooks = hooks;
		this.kernel = kernel;
		this.net = net;
		this.scheduler = scheduler;
		this.world = world;
	}

	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			this.scheduler.tick();
			Hooks hooks = this.hooks.get();
			if (hooks != null && hooks.has(CoreHooks.CLIENT_TICK)) {
				hooks.fire(CoreHooks.CLIENT_TICK, new ClientTickEvent(client.level != null));
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			Hooks hooks = this.hooks.get();
			if (hooks != null) {
				hooks.fire(CoreHooks.WORLD_JOIN, new WorldJoinEvent(this.world.isSingleplayer(), this.world.serverAddress()));
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			NetService net = this.net.get();
			if (net != null) {
				net.resetClient();
			}
			Hooks hooks = this.hooks.get();
			if (hooks != null) {
				hooks.fire(CoreHooks.WORLD_LEAVE, new WorldLeaveEvent());
			}
		});
		//? if >=1.20.5 {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> this.tooltip(stack, lines, flag.isAdvanced()));
		//?} else
		//ItemTooltipCallback.EVENT.register((stack, flag, lines) -> this.tooltip(stack, lines, flag.isAdvanced()));
		//? if >=1.19.3
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !this.chat(message, overlay));
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			//? if >=1.21.9 {
			ScreenKeyboardEvents.allowKeyPress(screen).register((target, event) -> !this.screenKey(target, new KeyInput(McInputUtil.toCore(event.key(), event.keycode()), event.keycode(), McInputUtil.modifiers(event.modifiers()))));
			//?} else
			//ScreenKeyboardEvents.allowKeyPress(screen).register((target, key, scancode, modifiers) -> !this.screenKey(target, new KeyInput(McInputUtil.toCore(key, scancode), scancode, McInputUtil.modifiers(modifiers))));
			this.tabs(screen);
			if (screen instanceof ChatScreen) {
				new McChatWarning((ChatScreen) screen).register();
			}
		});
	}

	private void tabs(Screen screen) {
		Kernel kernel = this.kernel.get();
		TabScreen target = this.tabScreen(screen);
		if (kernel == null || target == null) {
			return;
		}
		InventoryTabs service = kernel.services().find(InventoryTabs.class).orElse(null);
		if (service == null || service.position().hidden()) {
			return;
		}
		List<InventoryTab> shown = service.shown(target);
		if (!shown.isEmpty()) {
			new McTabBar(screen, service.position(), shown, kernel.platform().client().sounds()).register();
		}
	}

	private TabScreen tabScreen(Screen screen) {
		if (screen instanceof HostScreen host) {
			return new TabScreen(TabScreenKind.MOD_SCREEN, host.ui());
		}
		if (screen instanceof CreativeModeInventoryScreen) {
			return new TabScreen(TabScreenKind.CREATIVE_INVENTORY, null);
		}
		if (screen instanceof InventoryScreen) {
			return new TabScreen(TabScreenKind.INVENTORY, null);
		}
		return null;
	}

	private boolean screenKey(Screen screen, KeyInput input) {
		Hooks hooks = this.hooks.get();
		if (hooks == null || !hooks.has(CoreHooks.SCREEN_KEY)) {
			return false;
		}
		boolean modScreen = screen instanceof HostScreen;
		boolean typing = modScreen ? ((HostScreen) screen).ui().typing() : screen.getFocused() instanceof EditBox;
		return hooks.fire(CoreHooks.SCREEN_KEY, new ScreenKeyEvent(input, McScreenUtil.hovered(screen), typing, modScreen)).cancelled();
	}

	public boolean slotClicked(Screen screen, Slot slot) {
		Hooks hooks = this.hooks.get();
		if (hooks == null || !hooks.has(CoreHooks.SLOT_CLICK) || !(screen instanceof AbstractContainerScreen)) {
			return false;
		}
		HoveredItem hovered = McScreenUtil.hovered(slot);
		ItemStack cursor = ((AbstractContainerScreen<?>) screen).getMenu().getCarried();
		if (hovered == null || cursor.isEmpty()) {
			return false;
		}
		return hooks.fire(CoreHooks.SLOT_CLICK, new SlotClickEvent(hovered, FabricGameItem.of(cursor), McInputUtil.currentModifiers())).cancelled();
	}

	public void filesDropped(Screen screen, List<Path> paths) {
		Hooks hooks = this.hooks.get();
		if (hooks == null || !hooks.has(CoreHooks.FILES_DROP) || paths.isEmpty()) {
			return;
		}
		DropScreen kind;
		if (screen instanceof HostScreen) {
			kind = DropScreen.MOD_SCREEN;
		} else if (screen instanceof AbstractContainerScreen) {
			kind = DropScreen.CONTAINER;
		} else if (screen instanceof PauseScreen) {
			kind = DropScreen.PAUSE;
		} else {
			kind = DropScreen.OTHER;
		}
		hooks.fire(CoreHooks.FILES_DROP, new FilesDropEvent(paths, kind));
	}

	public boolean chat(Component message, boolean overlay) {
		Hooks hooks = this.hooks.get();
		if (hooks == null || !hooks.has(CoreHooks.CHAT_RECEIVE)) {
			return false;
		}
		return hooks.fire(CoreHooks.CHAT_RECEIVE, new ChatReceiveEvent(McTextUtil.fromMc(message), overlay)).cancelled();
	}

	private void tooltip(ItemStack stack, List<Component> lines, boolean advanced) {
		Hooks hooks = this.hooks.get();
		if (hooks == null || !hooks.has(CoreHooks.ITEM_TOOLTIP)) {
			return;
		}
		List<Text> converted = new ArrayList<>(lines.size());
		for (Component line : lines) {
			converted.add(McTextUtil.fromMc(line));
		}
		List<Text> original = List.copyOf(converted);
		hooks.fire(CoreHooks.ITEM_TOOLTIP, new TooltipEvent(FabricGameItem.of(stack), converted, advanced));
		if (converted.equals(original)) {
			return;
		}
		int common = 0;
		while (common < original.size() && common < converted.size() && original.get(common).equals(converted.get(common))) {
			common++;
		}
		while (lines.size() > common) {
			lines.remove(lines.size() - 1);
		}
		for (int i = common; i < converted.size(); i++) {
			lines.add(McTextUtil.toMc(converted.get(i)));
		}
	}
}
