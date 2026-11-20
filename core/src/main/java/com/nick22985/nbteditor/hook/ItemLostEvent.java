package com.nick22985.nbteditor.hook;

import com.nick22985.nbteditor.platform.GameItem;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ItemLostEvent(GameItem item, LostItemReason reason) {
}
