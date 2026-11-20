package com.nick22985.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class SkullUtilTest {
	@Test
	void skinIdIgnoresTheRestOfTheTexture() {
		String plain = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abc123\"}}}";
		String stamped = "{\"timestamp\":1,\"profileName\":\"Bob\",\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/abc123\"}}}";
		assertEquals("abc123", SkullUtil.skinId(this.encode(plain)));
		assertEquals("abc123", SkullUtil.skinId(this.encode(stamped)));
	}

	@Test
	void skinIdFallsBackToTheValue() {
		assertEquals("not base64!", SkullUtil.skinId("not base64!"));
		assertEquals(this.encode("{}"), SkullUtil.skinId(this.encode("{}")));
	}

	private String encode(String json) {
		return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
