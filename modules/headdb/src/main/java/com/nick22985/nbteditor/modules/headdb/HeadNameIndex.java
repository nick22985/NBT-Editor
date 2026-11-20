package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.data.HeadNames;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.SkullUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadNameIndex implements HeadNames {
	private final HeadDatabase database;
	private final HeadItems items;
	private Map<String, Head> index = Map.of();
	private int version = -1;

	public HeadNameIndex(HeadDatabase database, HeadItems items) {
		this.database = database;
		this.items = items;
	}

	@Override
	public synchronized Optional<Text> name(String texture) {
		if (texture == null || texture.isEmpty()) {
			return Optional.empty();
		}
		this.database.warm();
		int current = this.database.version();
		if (current != this.version) {
			Map<String, Head> built = new HashMap<>();
			for (Head head : this.database.heads(HeadCategory.ALL)) {
				built.putIfAbsent(SkullUtil.skinId(head.value()), head);
			}
			this.index = built;
			this.version = current;
		}
		Head head = this.index.get(SkullUtil.skinId(texture));
		return head == null ? Optional.empty() : Optional.of(this.items.name(head));
	}
}
