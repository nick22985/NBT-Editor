package com.nick22985.nbteditor.modules.editor.tree;

import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.util.TagArrayUtil;
import com.nick22985.nbteditor.util.TagDisplayUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TreeRow {
	private final NbtPath path;
	private final int depth;
	private final String key;
	private final boolean listIndex;
	private Tag tag;
	private boolean expanded;
	private boolean match;
	private boolean invalid;
	private Text value;

	public TreeRow(NbtPath path, int depth, String key, boolean listIndex, Tag tag) {
		this.path = path;
		this.depth = depth;
		this.key = key;
		this.listIndex = listIndex;
		this.tag = tag;
	}

	public NbtPath path() {
		return this.path;
	}

	public int depth() {
		return this.depth;
	}

	public String key() {
		return this.key;
	}

	public boolean listIndex() {
		return this.listIndex;
	}

	public Tag tag() {
		return this.tag;
	}

	public void tag(Tag tag) {
		this.tag = tag;
		this.value = null;
	}

	public boolean container() {
		return this.tag.type().isContainer();
	}

	public boolean expandable() {
		return this.container() && TagArrayUtil.containerSize(this.tag) > 0;
	}

	public boolean expanded() {
		return this.expanded;
	}

	public void expanded(boolean expanded) {
		this.expanded = expanded;
	}

	public boolean match() {
		return this.match;
	}

	public void match(boolean match) {
		this.match = match;
	}

	public boolean invalid() {
		return this.invalid;
	}

	public void invalid(boolean invalid) {
		this.invalid = invalid;
	}

	public Text value(SnbtDialect dialect) {
		if (this.value == null) {
			this.value = this.container() ? TextStyleUtil.colored(TagDisplayUtil.summary(this.tag), Theme.FAINT) : TagDisplayUtil.inline(this.tag, dialect);
		}
		return this.value;
	}
}
