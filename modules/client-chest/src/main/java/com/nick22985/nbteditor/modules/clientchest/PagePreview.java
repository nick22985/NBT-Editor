package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.platform.GameItem;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PagePreview(ClientChestPage page, List<GameItem> items) {
}
