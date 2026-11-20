package com.nick22985.nbteditor.fabric.util;

//? if >=1.21.11 {
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
//?}
import net.minecraft.world.entity.player.Player;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McPermissionUtil {
	private McPermissionUtil() {
	}

	public static int level(Player player) {
		if (player == null) {
			return 0;
		}
		//? if >=1.21.11 {
		PermissionSet permissions = player.permissions();
		if (permissions instanceof LevelBasedPermissionSet levels) {
			return levels.level().id();
		}
		if (permissions.hasPermission(Permissions.COMMANDS_OWNER)) {
			return 4;
		}
		if (permissions.hasPermission(Permissions.COMMANDS_ADMIN)) {
			return 3;
		}
		if (permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			return 2;
		}
		if (permissions.hasPermission(Permissions.COMMANDS_MODERATOR)) {
			return 1;
		}
		return 0;
		//?} else {
		/*for (int level = 4; level > 0; level--) {
			if (player.hasPermissions(level)) {
				return level;
			}
		}
		return 0;
		*///?}
	}
}
