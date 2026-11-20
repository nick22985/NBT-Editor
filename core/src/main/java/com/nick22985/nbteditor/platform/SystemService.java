package com.nick22985.nbteditor.platform;

import java.nio.file.Path;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface SystemService {
	void openFile(Path path);

	void openUrl(String url);

	String clipboard();

	void setClipboard(String text);
}
