package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.net.Transport;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Platform {
	GameInfo game();

	Log log();

	Scheduler scheduler();

	Transport transport();

	ClientPlatform client();

	ServerPlatform server();
}
