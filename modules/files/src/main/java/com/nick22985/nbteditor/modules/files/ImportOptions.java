package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.platform.Position;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ImportOptions(int dataVersion, int imageWidth, int imageHeight, Position at) {
}
