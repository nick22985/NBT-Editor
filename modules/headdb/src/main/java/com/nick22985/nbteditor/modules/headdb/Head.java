package com.nick22985.nbteditor.modules.headdb;

import java.util.List;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record Head(String name, UUID uuid, String value, List<String> tags, HeadCategory category) {
}
