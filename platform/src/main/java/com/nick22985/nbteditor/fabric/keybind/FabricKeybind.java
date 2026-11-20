package com.nick22985.nbteditor.fabric.keybind;

import com.nick22985.nbteditor.fabric.util.McInputUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.keybind.Keybind;
import com.nick22985.nbteditor.keybind.KeybindSpec;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.KeyInput;
import net.minecraft.client.KeyMapping;
//? if >=1.21.9
import net.minecraft.client.input.KeyEvent;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricKeybind implements Keybind {
	private final KeybindSpec spec;
	private final KeyMapping mapping;

	public FabricKeybind(KeybindSpec spec, KeyMapping mapping) {
		this.spec = spec;
		this.mapping = mapping;
	}

	public KeyMapping mapping() {
		return this.mapping;
	}

	@Override
	public KeybindSpec spec() {
		return this.spec;
	}

	@Override
	public boolean isDown() {
		return this.mapping.isDown();
	}

	@Override
	public boolean consumeClick() {
		return this.mapping.consumeClick();
	}

	@Override
	public boolean matches(KeyInput input) {
		int nativeKey = McInputUtil.toNative(input.key());
		//? if >=1.21.9 {
		return this.mapping.matches(new KeyEvent(nativeKey, input.scancode(), 0));
		//?} else
		//return this.mapping.matches(nativeKey, input.scancode());
	}

	@Override
	public Text boundKey() {
		return McTextUtil.fromMc(this.mapping.getTranslatedKeyMessage());
	}
}
