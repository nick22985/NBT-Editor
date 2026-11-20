package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.command.CommandService;
import com.nick22985.nbteditor.keybind.KeybindService;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ClientPlatform {
	ItemService items();

	RegistryService registries();

	PlayerService player();

	WorldService world();

	ChatService chat();

	SoundService sounds();

	SystemService system();

	ScreenService screens();

	CommandService commands();

	KeybindService keybinds();

	NbtCompletionService completions();

	DataFixService dataFixes();

	ResourceService resources();
}
