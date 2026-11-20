package com.nick22985.nbteditor.fabric.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
	@Accessor("input")
	EditBox nbtedit$input();
}
