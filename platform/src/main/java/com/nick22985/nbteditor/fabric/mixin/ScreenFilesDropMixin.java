package com.nick22985.nbteditor.fabric.mixin;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@Mixin(Screen.class)
public abstract class ScreenFilesDropMixin {
	@Inject(method = "onFilesDrop", at = @At("HEAD"))
	private void nbtedit$onFilesDrop(List<Path> paths, CallbackInfo info) {
		NbteRuntime.INSTANCE.filesDropped((Screen) (Object) this, paths);
	}
}
