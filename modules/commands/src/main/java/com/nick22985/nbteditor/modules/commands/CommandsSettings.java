package com.nick22985.nbteditor.modules.commands;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandsSettings {
	private final Setting<Boolean> extremeAliases;
	private final Setting<Map<String, String>> aliases;

	public CommandsSettings(ModuleContext ctx) {
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put("nbteditor", "nbt");
		defaults.put("itemfactory", "factory");
		defaults.put("chest", "clientchest");
		defaults.put("storage", "clientchest");
		defaults.put("sign", "factory signature");
		this.extremeAliases = ctx.config().bool("extreme_aliases", false);
		this.aliases = ctx.config().stringMap("aliases", defaults);
	}

	public boolean extremeAliases() {
		return this.extremeAliases.get();
	}

	public Map<String, String> aliases() {
		return this.aliases.get();
	}
}
