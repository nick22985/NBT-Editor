package com.nick22985.nbteditor.container;

import com.nick22985.nbteditor.platform.GameItem;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ItemStash {
	CompletableFuture<Integer> insert(GameItem item);
}
