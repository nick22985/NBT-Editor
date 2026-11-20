package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.text.Text;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record EntityHit(String dimension, UUID uuid, int networkId, String typeId, Text name) implements Hit {
}
