package com.nick22985.nbteditor.fabric.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.fabric.util.McInputUtil;
import com.nick22985.nbteditor.keybind.Keybind;
import com.nick22985.nbteditor.keybind.KeybindService;
import com.nick22985.nbteditor.keybind.KeybindSpec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else
//import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricKeybindService implements KeybindService {
	private final Map<String, FabricKeybind> keybinds = new HashMap<>();
	//? if >=1.21.9
	private final Map<String, KeyMapping.Category> categories = new HashMap<>();

	@Override
	public Keybind register(KeybindSpec spec) {
		FabricKeybind existing = this.keybinds.get(spec.id());
		if (existing != null) {
			return existing;
		}
		int key = spec.defaultKey() == KeybindSpec.UNBOUND ? InputConstants.UNKNOWN.getValue() : McInputUtil.toNative(spec.defaultKey());
		//? if >=1.21.9 {
		KeyMapping.Category category = this.categories.computeIfAbsent(spec.category(), name -> KeyMapping.Category.register(McIdUtil.of("nbtedit", name)));
		//?} else
		//String category = "key.category.nbtedit." + spec.category();
		KeyMapping mapping = new KeyMapping(spec.translationKey(), InputConstants.Type.KEYBOARD, key, category);
		//? if >=26.1 {
		KeyMappingHelper.registerKeyMapping(mapping);
		//?} else
		//KeyBindingHelper.registerKeyBinding(mapping);
		FabricKeybind keybind = new FabricKeybind(spec, mapping);
		this.keybinds.put(spec.id(), keybind);
		return keybind;
	}
}
