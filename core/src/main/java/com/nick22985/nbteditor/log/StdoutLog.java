package com.nick22985.nbteditor.log;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StdoutLog implements Log {
	private final String name;

	public StdoutLog(String name) {
		this.name = name;
	}

	private void print(String level, String message, Throwable error) {
		System.out.println("[" + this.name + "/" + level + "] " + message);
		if (error != null) {
			error.printStackTrace(System.out);
		}
	}

	@Override
	public void info(String message) {
		this.print("INFO", message, null);
	}

	@Override
	public void warn(String message) {
		this.print("WARN", message, null);
	}

	@Override
	public void warn(String message, Throwable error) {
		this.print("WARN", message, error);
	}

	@Override
	public void error(String message, Throwable error) {
		this.print("ERROR", message, error);
	}

	@Override
	public void debug(String message) {
		if (Boolean.getBoolean("nbtedit.debug")) {
			this.print("DEBUG", message, null);
		}
	}

	@Override
	public Log child(String name) {
		return new StdoutLog(this.name + "/" + name);
	}
}
