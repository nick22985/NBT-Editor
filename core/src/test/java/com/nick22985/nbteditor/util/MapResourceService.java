package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.platform.ResourceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
final class MapResourceService implements ResourceService {
	private final Map<String, String> files;

	MapResourceService(Map<String, String> files) {
		this.files = files;
	}

	@Override
	public String read(String id) {
		return this.files.get(id);
	}

	@Override
	public List<String> list(String folder) {
		List<String> out = new ArrayList<>();
		for (String id : this.files.keySet()) {
			if (id.substring(id.indexOf(':') + 1).startsWith(folder + "/")) {
				out.add(id);
			}
		}
		return out;
	}

	@Override
	public String pack(String id) {
		return this.files.containsKey(id) ? "file/" + id.substring(0, id.indexOf(':')) + ".zip" : null;
	}

	@Override
	public List<String> packs() {
		return List.of();
	}
}
