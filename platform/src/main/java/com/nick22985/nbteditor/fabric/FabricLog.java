package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.log.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricLog implements Log {
	private final String name;
	private final Logger logger;

	public FabricLog(String name) {
		this.name = name;
		this.logger = LogManager.getLogger(name);
	}

	@Override
	public void info(String message) {
		this.logger.info(message);
	}

	@Override
	public void warn(String message) {
		this.logger.warn(message);
	}

	@Override
	public void warn(String message, Throwable error) {
		this.logger.warn(message, error);
	}

	@Override
	public void error(String message, Throwable error) {
		this.logger.error(message, error);
	}

	@Override
	public void debug(String message) {
		if (Boolean.getBoolean("nbtedit.debug")) {
			this.logger.info("[debug] " + message);
		} else {
			this.logger.debug(message);
		}
	}

	@Override
	public Log child(String name) {
		return new FabricLog(this.name + "/" + name);
	}
}
