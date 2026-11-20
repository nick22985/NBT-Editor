package com.nick22985.nbteditor.modules.editor.tree;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.TagArrayUtil;
import com.nick22985.nbteditor.util.TagDisplayUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TreeModel {
	private final Set<NbtPath> expanded = new HashSet<>();
	private final Map<NbtPath, Integer> index = new HashMap<>();
	private List<TreeRow> rows = List.of();
	private Set<NbtPath> invalid = Set.of();
	private String query;
	private boolean searching;
	private SnbtDialect dialect = SnbtDialect.MODERN;

	public List<TreeRow> rows() {
		return this.rows;
	}

	public int indexOf(NbtPath path) {
		Integer found = this.index.get(path);
		return found == null ? -1 : found;
	}

	public TreeRow row(NbtPath path) {
		int found = this.indexOf(path);
		return found < 0 ? null : this.rows.get(found);
	}

	public List<NbtPath> order() {
		List<NbtPath> out = new ArrayList<>(this.rows.size());
		for (TreeRow row : this.rows) {
			out.add(row.path());
		}
		return out;
	}

	public boolean searching() {
		return this.searching;
	}

	public void query(String query) {
		String trimmed = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		this.query = trimmed.isEmpty() ? null : trimmed;
	}

	public void dialect(SnbtDialect dialect) {
		this.dialect = dialect;
	}

	public void invalid(Set<NbtPath> invalid) {
		this.invalid = invalid;
	}

	public boolean isExpanded(NbtPath path) {
		return this.expanded.contains(path);
	}

	public void expand(NbtPath path) {
		this.expanded.add(path);
	}

	public void collapse(NbtPath path) {
		this.expanded.remove(path);
	}

	public void toggle(NbtPath path) {
		if (!this.expanded.remove(path)) {
			this.expanded.add(path);
		}
	}

	public void collapseAll() {
		this.expanded.clear();
	}

	public void expandAll(Tag root, NbtPath base) {
		Tag start = base.get(root);
		if (start != null) {
			this.expandTree(start, base);
		}
	}

	public void collapseUnder(NbtPath path) {
		this.expanded.removeIf(candidate -> candidate.startsWith(path));
	}

	public void expandUnder(Tag root, NbtPath path) {
		Tag start = path.get(root);
		if (start != null) {
			this.expandTree(start, path);
		}
	}

	public void reveal(NbtPath base, NbtPath path) {
		for (NbtPath current = path.parent(); current.size() > base.size(); current = current.parent()) {
			this.expanded.add(current);
		}
	}

	private void expandTree(Tag tag, NbtPath path) {
		if (!tag.type().isContainer() || tag.type().isArray()) {
			return;
		}
		this.expanded.add(path);
		int size = TagArrayUtil.containerSize(tag);
		if (tag instanceof CompoundTag) {
			for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
				this.expandTree(entry.getValue(), path.child(entry.getKey()));
			}
		} else {
			for (int i = 0; i < size; i++) {
				this.expandTree(TagDisplayUtil.childOf(tag, i), path.child(i));
			}
		}
	}

	public void build(Tag root, NbtPath base) {
		List<TreeRow> out = new ArrayList<>();
		Tag start = base.get(root);
		this.searching = this.query != null;
		if (start != null && start.type().isContainer()) {
			this.walk(start, base, 0, out);
		}
		this.rows = Collections.unmodifiableList(out);
		this.index.clear();
		for (int i = 0; i < out.size(); i++) {
			this.index.put(out.get(i).path(), i);
		}
	}

	public boolean update(Tag root, NbtPath path) {
		TreeRow row = this.row(path);
		if (row == null) {
			return false;
		}
		Tag tag = path.get(root);
		if (tag == null || tag.type() != row.tag().type()) {
			return false;
		}
		row.tag(tag);
		return true;
	}

	private boolean walk(Tag container, NbtPath path, int depth, List<TreeRow> out) {
		boolean any = false;
		if (container instanceof CompoundTag) {
			for (Map.Entry<String, Tag> entry : ((CompoundTag) container).entries()) {
				any |= this.visit(entry.getValue(), path.child(entry.getKey()), depth, entry.getKey(), false, out);
			}
			return any;
		}
		int size = TagArrayUtil.containerSize(container);
		for (int i = 0; i < size; i++) {
			any |= this.visit(TagDisplayUtil.childOf(container, i), path.child(i), depth, Integer.toString(i), true, out);
		}
		return any;
	}

	private boolean visit(Tag tag, NbtPath path, int depth, String key, boolean listIndex, List<TreeRow> out) {
		int at = out.size();
		TreeRow row = new TreeRow(path, depth, key, listIndex, tag);
		row.invalid(this.invalid.contains(path));
		out.add(row);
		if (this.query == null) {
			if (row.expandable() && this.expanded.contains(path)) {
				row.expanded(true);
				this.walk(tag, path, depth + 1, out);
			}
			return true;
		}
		boolean self = TagDisplayUtil.matches(this.query, key) || !tag.type().isContainer() && TagDisplayUtil.matches(this.query, TagDisplayUtil.editText(tag, this.dialect));
		boolean below = row.expandable() && this.walk(tag, path, depth + 1, out);
		if (!self && !below) {
			out.subList(at, out.size()).clear();
			return false;
		}
		row.match(self);
		row.expanded(below);
		return true;
	}
}
