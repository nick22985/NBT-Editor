package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.platform.SoundService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricSoundService implements SoundService {
	@Override
	public void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public void play(String soundId, float volume, float pitch) {
		Identifier id = McIdUtil.tryParse(soundId);
		if (id == null) {
			return;
		}
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
		if (sound != null) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
		}
	}
}
