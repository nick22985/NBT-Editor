package com.nick22985.nbteditor.log;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Log {
	void info(String message);

	void warn(String message);

	void warn(String message, Throwable error);

	void error(String message, Throwable error);

	void debug(String message);

	Log child(String name);
}
