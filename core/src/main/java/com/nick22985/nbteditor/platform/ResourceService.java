package com.nick22985.nbteditor.platform;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ResourceService {
	String read(String id);

	List<String> list(String folder);

	String pack(String id);

	List<String> packs();
}
