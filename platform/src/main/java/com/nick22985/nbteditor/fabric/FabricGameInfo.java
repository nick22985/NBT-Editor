package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.fabric.util.McVersionUtil;
import com.nick22985.nbteditor.platform.GameInfo;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricGameInfo implements GameInfo {
	public static final String MOD_ID = "nbtedit";

	@Override
	public GameVersion version() {
		return McVersionUtil.current();
	}

	@Override
	public String versionName() {
		return McVersionUtil.name();
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	}

	@Override
	public Path gameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean isDevelopment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public String modVersion() {
		return FabricLoader.getInstance().getModContainer(MOD_ID).map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}

	@Override
	public boolean isClient() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}
}
