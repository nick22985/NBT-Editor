package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.data.GameVersion;
import java.nio.file.Path;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface GameInfo {
	GameVersion version();

	String versionName();

	Path configDir();

	Path gameDir();

	boolean isModLoaded(String modId);

	boolean isDevelopment();

	String modVersion();

	boolean isClient();
}
