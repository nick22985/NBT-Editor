package com.nick22985.nbteditor.doc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
final class EditTransaction {
	private final String label;
	private final String coalesceKey;
	private final List<Edit> edits = new ArrayList<>();

	EditTransaction(String label, String coalesceKey) {
		this.label = label;
		this.coalesceKey = coalesceKey;
	}

	String label() {
		return label;
	}

	String coalesceKey() {
		return coalesceKey;
	}

	List<Edit> edits() {
		return edits;
	}
}
