package com.nick22985.nbteditor.hook;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record TooltipEvent(GameItem item, List<Text> lines, boolean advanced) {
}
