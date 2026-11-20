package com.nick22985.nbteditor.ui.text;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record RichSnapshot(List<RichCell> cells, int cursor, int anchor) {
}
