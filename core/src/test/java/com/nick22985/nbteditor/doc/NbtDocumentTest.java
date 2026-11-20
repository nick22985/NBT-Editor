package com.nick22985.nbteditor.doc;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.StringTag;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class NbtDocumentTest {
	private static CompoundTag sample() {
		CompoundTag root = new CompoundTag().putInt("a", 1).putString("b", "two").putInt("c", 3);
		root.put("list", ListTag.of(IntTag.of(10), IntTag.of(20), IntTag.of(30)));
		root.put("arr", new IntArrayTag(new int[] {1, 2, 3}));
		return root;
	}

	@Test
	void setUndoRedo() {
		CompoundTag original = sample();
		NbtDocument doc = new NbtDocument(original.copy());
		doc.set(NbtPath.of("a"), IntTag.of(5));
		doc.set(NbtPath.of("new"), StringTag.of("x"));
		assertEquals(IntTag.of(5), doc.get(NbtPath.of("a")));
		assertTrue(doc.dirty());
		doc.undo();
		doc.undo();
		assertEquals(original, doc.root());
		assertFalse(doc.dirty());
		doc.redo();
		assertEquals(IntTag.of(5), doc.get(NbtPath.of("a")));
	}

	@Test
	void removeRestoresOrder() {
		CompoundTag original = sample();
		NbtDocument doc = new NbtDocument(original.copy());
		doc.remove(NbtPath.of("b"));
		doc.undo();
		assertEquals(List.copyOf(original.keys()), List.copyOf(((CompoundTag) doc.root()).keys()));
	}

	@Test
	void renameKeepsPosition() {
		NbtDocument doc = new NbtDocument(sample());
		doc.rename(NbtPath.of("b"), "bee");
		assertEquals(1, ((CompoundTag) doc.root()).indexOf("bee"));
		doc.undo();
		assertEquals(1, ((CompoundTag) doc.root()).indexOf("b"));
		assertThrows(EditException.class, () -> doc.rename(NbtPath.of("a"), "c"));
	}

	@Test
	void listAndArrayEdits() {
		CompoundTag original = sample();
		NbtDocument doc = new NbtDocument(original.copy());
		doc.insert(NbtPath.of("list", 1), IntTag.of(15));
		doc.remove(NbtPath.of("list", 0));
		doc.set(NbtPath.of("arr", 1), IntTag.of(9));
		doc.insert(NbtPath.of("arr", 3), IntTag.of(4));
		doc.remove(NbtPath.of("arr", 0));
		assertEquals(ListTag.of(IntTag.of(15), IntTag.of(20), IntTag.of(30)), doc.get(NbtPath.of("list")));
		assertEquals(new IntArrayTag(new int[] {9, 3, 4}), doc.get(NbtPath.of("arr")));
		while (doc.undo()) {
		}
		assertEquals(original, doc.root());
	}

	@Test
	void batchIsOneStepAndRollsBack() {
		CompoundTag original = sample();
		NbtDocument doc = new NbtDocument(original.copy());
		doc.batch("multi", () -> {
			doc.remove(NbtPath.of("a"));
			doc.remove(NbtPath.of("c"));
		});
		doc.undo();
		assertEquals(original, doc.root());
		assertThrows(EditException.class, () -> doc.batch("bad", () -> {
			doc.remove(NbtPath.of("a"));
			doc.rename(NbtPath.of("b"), "c");
		}));
		assertEquals(original, doc.root());
		assertFalse(doc.canRedo() && doc.undoLabel() != null && doc.undoLabel().equals("bad"));
	}

	@Test
	void coalescedEditsMerge() {
		NbtDocument doc = new NbtDocument(sample());
		doc.batch("type", "field:b", () -> doc.set(NbtPath.of("b"), StringTag.of("t")));
		doc.batch("type", "field:b", () -> doc.set(NbtPath.of("b"), StringTag.of("th")));
		doc.batch("type", "field:b", () -> doc.set(NbtPath.of("b"), StringTag.of("thr")));
		doc.undo();
		assertEquals(StringTag.of("two"), doc.get(NbtPath.of("b")));
		assertFalse(doc.canUndo());
	}

	@Test
	void eventsAndSelection() {
		NbtDocument doc = new NbtDocument(sample());
		List<DocumentEvent> events = new ArrayList<>();
		doc.addListener(events::add);
		doc.selection().select(NbtPath.of("list", 2));
		doc.selection().toggle(NbtPath.of("list"));
		assertEquals(List.of(NbtPath.of("list")), doc.selection().roots());
		doc.remove(NbtPath.of("list", 2));
		assertEquals(1, events.size());
		assertTrue(events.get(0).structural());
		assertFalse(doc.selection().contains(NbtPath.of("list", 2)));
	}

	@Test
	void moveWithinList() {
		NbtDocument doc = new NbtDocument(sample());
		doc.move(NbtPath.of("list", 0), NbtPath.of("list", 3));
		assertEquals(ListTag.of(IntTag.of(20), IntTag.of(30), IntTag.of(10)), doc.get(NbtPath.of("list")));
		doc.undo();
		assertEquals(ListTag.of(IntTag.of(10), IntTag.of(20), IntTag.of(30)), doc.get(NbtPath.of("list")));
	}
}
