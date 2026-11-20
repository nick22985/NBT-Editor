package com.nick22985.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class FolderClusterUtilTest {
	@Test
	void groupsUnderAMatchingFolder() {
		Map<String, String> paths = FolderClusterUtil.paths(List.of("abyssal_warden", "abyssal_warden_slash_vfx_1", "abyssal_warden_slash_vfx_2", "abyssal_warden_slash_vfx_3", "abyssal_warden_thrust_vfx_1", "abyssal_warden_thrust_vfx_2", "alphamush"));
		assertEquals("abyssal_warden", paths.get("abyssal_warden"));
		assertEquals("abyssal_warden/slash_vfx/1", paths.get("abyssal_warden_slash_vfx_1"));
		assertEquals("abyssal_warden/slash_vfx/3", paths.get("abyssal_warden_slash_vfx_3"));
		assertEquals("abyssal_warden/thrust_vfx_1", paths.get("abyssal_warden_thrust_vfx_1"));
		assertEquals("alphamush", paths.get("alphamush"));
	}

	@Test
	void groupsThreeSharingAPrefix() {
		Map<String, String> paths = FolderClusterUtil.paths(List.of("asph_fx1", "asph_fx2", "asph_projectile", "baby_appa", "baby_dragon", "black_cat", "blackjack"));
		assertEquals("asph/fx1", paths.get("asph_fx1"));
		assertEquals("asph/projectile", paths.get("asph_projectile"));
		assertEquals("baby_appa", paths.get("baby_appa"));
		assertEquals("black_cat", paths.get("black_cat"));
		assertEquals("blackjack", paths.get("blackjack"));
	}

	@Test
	void keepsRealSubfolders() {
		Map<String, String> paths = FolderClusterUtil.paths(List.of("weapons/sword_1", "weapons/sword_2", "weapons/sword_3", "weapons/bow", "boar", "boar_tame"));
		assertEquals("weapons", paths.get("weapons"));
		assertEquals("weapons/sword/2", paths.get("weapons/sword_2"));
		assertEquals("weapons/bow", paths.get("weapons/bow"));
		assertEquals("boar", paths.get("boar"));
		assertEquals("boar/tame", paths.get("boar_tame"));
		assertEquals("", paths.get(""));
	}

	@Test
	void leavesALoneSharedPrefixFlat() {
		Map<String, String> paths = FolderClusterUtil.paths(List.of("fx_1", "fx_2", "fx_3"));
		assertEquals("fx_1", paths.get("fx_1"));
		assertEquals("fx_3", paths.get("fx_3"));
	}
}
