package com.nick22985.nbteditor.config;

import com.nick22985.nbteditor.log.StdoutLog;
import com.nick22985.nbteditor.target.SubjectKind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class ConfigStoreTest {
	@TempDir
	Path dir;

	@Test
	void valuesSurviveReload() {
		Path file = this.dir.resolve("nbtedit/config.json");
		ConfigStore store = new ConfigStore(file, new StdoutLog("test"));
		store.load();
		ConfigSection section = store.section("editor");
		section.bool("flag", false).set(true);
		section.integer("size", 5, 1, 10).set(50);
		section.color("tint", 0xFF0000).set(0x00FF00);
		section.enumeration("kind", SubjectKind.ITEM).set(SubjectKind.BLOCK);
		section.stringList("names", List.of()).set(List.of("a", "b"));
		store.setModuleEnabled("headdb", false);
		store.save();
		assertTrue(Files.exists(file));

		ConfigStore reloaded = new ConfigStore(file, new StdoutLog("test"));
		reloaded.load();
		ConfigSection again = reloaded.section("editor");
		assertTrue(again.bool("flag", false).get());
		assertEquals(10, again.integer("size", 5, 1, 10).get());
		assertEquals(0x00FF00, again.color("tint", 0xFF0000).get());
		assertEquals(SubjectKind.BLOCK, again.enumeration("kind", SubjectKind.ITEM).get());
		assertEquals(List.of("a", "b"), again.stringList("names", List.of()).get());
		assertFalse(reloaded.moduleEnabled("headdb", true));
		assertTrue(reloaded.moduleEnabled("editor", true));
	}

	@Test
	void unknownSettingsArePreserved() throws Exception {
		Path file = this.dir.resolve("config.json");
		Files.writeString(file, "{\"settings\":{\"gone\":{\"x\":1},\"editor\":{\"old\":true}}}");
		ConfigStore store = new ConfigStore(file, new StdoutLog("test"));
		store.load();
		store.section("editor").bool("flag", false).set(true);
		store.save();
		String saved = Files.readString(file);
		assertTrue(saved.contains("\"gone\""));
		assertTrue(saved.contains("\"old\""));
	}

	@Test
	void brokenFileFallsBackToDefaults() throws Exception {
		Path file = this.dir.resolve("config.json");
		Files.writeString(file, "{not json");
		ConfigStore store = new ConfigStore(file, new StdoutLog("test"));
		store.load();
		assertTrue(store.section("editor").bool("flag", true).get());
		assertTrue(Files.exists(this.dir.resolve("config.json.broken")));
	}
}
