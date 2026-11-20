package com.nick22985.nbteditor.doc;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.NbtPathUtil;
import com.nick22985.nbteditor.util.TagArrayUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtDocument {
	private static final int DEFAULT_HISTORY = 256;

	private Tag root;
	private final Deque<EditTransaction> undo = new ArrayDeque<>();
	private final Deque<EditTransaction> redo = new ArrayDeque<>();
	private final List<Consumer<DocumentEvent>> listeners = new CopyOnWriteArrayList<>();
	private final Selection selection = new Selection();
	private int historyLimit = DEFAULT_HISTORY;
	private EditTransaction open;
	private int depth;
	private EditTransaction savedAt;
	private boolean savedAtStart = true;

	public NbtDocument(Tag root) {
		this.root = Objects.requireNonNull(root);
	}

	public Tag root() {
		return root;
	}

	public Selection selection() {
		return selection;
	}

	public Tag get(NbtPath path) {
		return path.get(root);
	}

	public void setHistoryLimit(int limit) {
		historyLimit = Math.max(1, limit);
		while (undo.size() > historyLimit) {
			undo.removeLast();
		}
	}

	public void addListener(Consumer<DocumentEvent> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<DocumentEvent> listener) {
		listeners.remove(listener);
	}

	public void reset(Tag newRoot) {
		root = Objects.requireNonNull(newRoot);
		undo.clear();
		redo.clear();
		savedAt = null;
		savedAtStart = true;
		selection.retainValid(root);
		fire(new DocumentEvent(DocumentCause.RESET, List.of()));
	}

	public void markSaved() {
		savedAt = undo.peekFirst();
		savedAtStart = savedAt == null;
		fire(new DocumentEvent(DocumentCause.SAVED, List.of()));
	}

	public boolean dirty() {
		EditTransaction top = undo.peekFirst();
		return savedAtStart ? top != null : top != savedAt;
	}

	public boolean canUndo() {
		return !undo.isEmpty();
	}

	public boolean canRedo() {
		return !redo.isEmpty();
	}

	public String undoLabel() {
		return undo.isEmpty() ? null : undo.peekFirst().label();
	}

	public String redoLabel() {
		return redo.isEmpty() ? null : redo.peekFirst().label();
	}

	public void batch(String label, Runnable body) {
		batch(label, null, body);
	}

	public void batch(String label, String coalesceKey, Runnable body) {
		if (depth++ == 0) {
			open = new EditTransaction(label, coalesceKey);
		}
		boolean ok = false;
		try {
			body.run();
			ok = true;
		} finally {
			if (--depth == 0) {
				EditTransaction finished = open;
				open = null;
				if (!ok) {
					List<Edit> applied = finished.edits();
					for (int i = applied.size() - 1; i >= 0; i--) {
						apply(applied.get(i).inverse());
					}
				} else if (!finished.edits().isEmpty()) {
					commit(finished);
				}
			}
		}
	}

	public void set(NbtPath path, Tag value) {
		value = Objects.requireNonNull(value).copy();
		if (path.isRoot()) {
			record(Edit.set(path, root, value, -1));
			return;
		}
		Tag parent = requireParent(path);
		Object last = path.last();
		if (parent instanceof CompoundTag) {
			CompoundTag compound = (CompoundTag) parent;
			Tag before = compound.get((String) last);
			record(Edit.set(path, before, value, before == null ? -1 : compound.indexOf((String) last)));
			return;
		}
		int index = (Integer) path.last();
		Tag before = NbtPathUtil.step(parent, index);
		if (before == null) {
			throw new EditException("index_out_of_range");
		}
		if (parent instanceof ListTag) {
			record(Edit.set(path, before, value, index));
		} else {
			record(Edit.set(path.parent(), parent, TagArrayUtil.arrayWith(parent, index, value), -1));
		}
	}

	public void insert(NbtPath path, Tag value) {
		value = Objects.requireNonNull(value).copy();
		Tag parent = requireParent(path);
		if (parent instanceof CompoundTag) {
			if (((CompoundTag) parent).contains((String) path.last())) {
				throw new EditException("key_exists");
			}
			record(Edit.insert(path, value));
			return;
		}
		int index = (Integer) path.last();
		int size = TagArrayUtil.containerSize(parent);
		if (index < 0 || index > size) {
			throw new EditException("index_out_of_range");
		}
		if (parent instanceof ListTag) {
			record(Edit.insert(path, value));
		} else {
			record(Edit.set(path.parent(), parent, TagArrayUtil.arrayInserted(parent, index, value), -1));
		}
	}

	public void remove(NbtPath path) {
		if (path.isRoot()) {
			throw new EditException("cannot_remove_root");
		}
		Tag parent = requireParent(path);
		if (parent instanceof CompoundTag) {
			CompoundTag compound = (CompoundTag) parent;
			Tag before = compound.get((String) path.last());
			if (before == null) {
				return;
			}
			record(Edit.remove(path, before, compound.indexOf((String) path.last())));
			return;
		}
		int index = (Integer) path.last();
		Tag before = NbtPathUtil.step(parent, index);
		if (before == null) {
			return;
		}
		if (parent instanceof ListTag) {
			record(Edit.remove(path, before, index));
		} else {
			record(Edit.set(path.parent(), parent, TagArrayUtil.arrayRemoved(parent, index), -1));
		}
	}

	public void rename(NbtPath path, String to) {
		Tag parent = requireParent(path);
		if (!(parent instanceof CompoundTag)) {
			throw new EditException("not_compound");
		}
		CompoundTag compound = (CompoundTag) parent;
		String from = (String) path.last();
		if (from.equals(to)) {
			return;
		}
		if (!compound.contains(from)) {
			throw new EditException("missing");
		}
		if (compound.contains(to)) {
			throw new EditException("key_exists");
		}
		record(Edit.rename(path.withLast(to), from));
	}

	public void move(NbtPath from, NbtPath to) {
		Tag value = get(from);
		if (value == null) {
			throw new EditException("missing");
		}
		if (to.startsWith(from)) {
			throw new EditException("into_self");
		}
		batch("move", () -> {
			NbtPath target = to;
			if (from.parent().equals(to.parent()) && from.last() instanceof Integer && to.last() instanceof Integer && (Integer) from.last() < (Integer) to.last()) {
				target = to.withLast((Integer) to.last() - 1);
			}
			remove(from);
			insert(target, value);
		});
	}

	public boolean undo() {
		EditTransaction transaction = undo.pollFirst();
		if (transaction == null) {
			return false;
		}
		List<Edit> inverse = new ArrayList<>(transaction.edits().size());
		for (int i = transaction.edits().size() - 1; i >= 0; i--) {
			Edit edit = transaction.edits().get(i).inverse();
			apply(edit);
			inverse.add(edit);
		}
		redo.addFirst(transaction);
		selection.retainValid(root);
		fire(new DocumentEvent(DocumentCause.UNDO, inverse));
		return true;
	}

	public boolean redo() {
		EditTransaction transaction = redo.pollFirst();
		if (transaction == null) {
			return false;
		}
		for (Edit edit : transaction.edits()) {
			apply(edit);
		}
		undo.addFirst(transaction);
		selection.retainValid(root);
		fire(new DocumentEvent(DocumentCause.REDO, Collections.unmodifiableList(transaction.edits())));
		return true;
	}

	private void record(Edit edit) {
		apply(edit);
		if (open != null) {
			open.edits().add(edit);
		} else {
			EditTransaction transaction = new EditTransaction(edit.kind().name().toLowerCase(), null);
			transaction.edits().add(edit);
			commit(transaction);
		}
	}

	private void commit(EditTransaction transaction) {
		redo.clear();
		EditTransaction top = undo.peekFirst();
		if (transaction.coalesceKey() != null && top != null && transaction.coalesceKey().equals(top.coalesceKey()) && top != savedAt) {
			top.edits().addAll(transaction.edits());
		} else {
			undo.addFirst(transaction);
			while (undo.size() > historyLimit) {
				EditTransaction dropped = undo.removeLast();
				if (dropped == savedAt) {
					savedAt = null;
					savedAtStart = false;
				}
			}
		}
		selection.retainValid(root);
		fire(new DocumentEvent(DocumentCause.EDIT, Collections.unmodifiableList(new ArrayList<>(transaction.edits()))));
	}

	private void apply(Edit edit) {
		NbtPath path = edit.path();
		if (path.isRoot()) {
			if (edit.kind() != EditKind.SET || edit.after() == null) {
				throw new EditException("bad_root_edit");
			}
			root = edit.after();
			return;
		}
		Tag parent = requireParent(path);
		Object last = path.last();
		switch (edit.kind()) {
			case SET:
				if (parent instanceof CompoundTag) {
					CompoundTag compound = (CompoundTag) parent;
					if (edit.after() == null) {
						compound.remove((String) last);
					} else if (!compound.contains((String) last) && edit.position() >= 0) {
						compound.putAt(edit.position(), (String) last, edit.after());
					} else {
						compound.put((String) last, edit.after());
					}
				} else {
					((ListTag) parent).set((Integer) last, edit.after());
				}
				break;
			case INSERT:
				if (parent instanceof CompoundTag) {
					((CompoundTag) parent).putAt(edit.position(), (String) last, edit.after());
				} else {
					((ListTag) parent).add((Integer) last, edit.after());
				}
				break;
			case REMOVE:
				if (parent instanceof CompoundTag) {
					((CompoundTag) parent).remove((String) last);
				} else {
					((ListTag) parent).remove((int) (Integer) last);
				}
				break;
			case RENAME:
				((CompoundTag) parent).rename(edit.renamedFrom(), (String) last);
				break;
		}
	}

	private Tag requireParent(NbtPath path) {
		if (path.isRoot()) {
			throw new EditException("root_has_no_parent");
		}
		Tag parent = path.parent().get(root);
		if (parent == null) {
			throw new EditException("missing_parent");
		}
		Object last = path.last();
		if (parent instanceof CompoundTag) {
			if (!(last instanceof String)) {
				throw new EditException("expected_key");
			}
		} else if (!(last instanceof Integer) || !(parent instanceof ListTag || parent.type().isArray())) {
			throw new EditException("not_container");
		}
		return parent;
	}

	private void fire(DocumentEvent event) {
		for (Consumer<DocumentEvent> listener : listeners) {
			listener.accept(event);
		}
	}
}
