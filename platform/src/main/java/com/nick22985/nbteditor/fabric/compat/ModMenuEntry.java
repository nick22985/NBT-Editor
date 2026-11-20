package com.nick22985.nbteditor.fabric.compat;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.client.FabricClientPlatform;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.ui.settings.SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModMenuEntry implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			Kernel kernel = NbteRuntime.INSTANCE.kernel();
			FabricClientPlatform platform = (FabricClientPlatform) kernel.platform().client();
			return new HostScreen(new SettingsScreen(kernel), parent, platform.host());
		};
	}
}
