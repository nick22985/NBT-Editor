package com.nick22985.nbteditor.hook;

import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PlayerJoinServerEvent(UUID player, String name) {
}
