package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.keybind.FabricKeybindService;
import com.nick22985.nbteditor.fabric.ui.McUiHost;
import com.nick22985.nbteditor.keybind.KeybindService;
import com.nick22985.nbteditor.platform.ChatService;
import com.nick22985.nbteditor.platform.ClientPlatform;
import com.nick22985.nbteditor.platform.DataFixService;
import com.nick22985.nbteditor.platform.ItemService;
import com.nick22985.nbteditor.platform.NbtCompletionService;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.platform.ResourceService;
import com.nick22985.nbteditor.platform.ScreenService;
import com.nick22985.nbteditor.platform.SoundService;
import com.nick22985.nbteditor.platform.SystemService;
import com.nick22985.nbteditor.platform.WorldService;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricClientPlatform implements ClientPlatform {
	private final ItemService items = new FabricItemService();
	private final RegistryService registries = new FabricRegistryService();
	private final PlayerService player = new FabricPlayerService();
	private final WorldService world = new FabricWorldService();
	private final ChatService chat = new FabricChatService();
	private final SoundService sounds = new FabricSoundService();
	private final SystemService system = new FabricSystemService();
	private final McUiHost host = new McUiHost(this.sounds, this.system);
	private final ScreenService screens = new FabricScreenService(this.host);
	private final FabricCommandService commands = new FabricCommandService(this.chat);
	private final KeybindService keybinds = new FabricKeybindService();
	private final NbtCompletionService completions = new FabricNbtCompletionService();
	private final DataFixService dataFixes = new FabricDataFixService();
	private final ResourceService resources = new FabricResourceService();

	public McUiHost host() {
		return this.host;
	}

	@Override
	public ItemService items() {
		return this.items;
	}

	@Override
	public RegistryService registries() {
		return this.registries;
	}

	@Override
	public PlayerService player() {
		return this.player;
	}

	@Override
	public WorldService world() {
		return this.world;
	}

	@Override
	public ChatService chat() {
		return this.chat;
	}

	@Override
	public SoundService sounds() {
		return this.sounds;
	}

	@Override
	public SystemService system() {
		return this.system;
	}

	@Override
	public ScreenService screens() {
		return this.screens;
	}

	@Override
	public FabricCommandService commands() {
		return this.commands;
	}

	@Override
	public KeybindService keybinds() {
		return this.keybinds;
	}

	@Override
	public NbtCompletionService completions() {
		return this.completions;
	}

	@Override
	public DataFixService dataFixes() {
		return this.dataFixes;
	}

	@Override
	public ResourceService resources() {
		return this.resources;
	}
}
