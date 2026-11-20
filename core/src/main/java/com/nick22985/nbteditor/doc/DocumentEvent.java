package com.nick22985.nbteditor.doc;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record DocumentEvent(DocumentCause cause, List<Edit> edits) {
	public boolean structural() {
		for (Edit edit : edits) {
			if (edit.kind() != EditKind.SET || edit.path().isRoot() || edit.before() == null || edit.after() == null || edit.before().type() != edit.after().type() || edit.after().type().isContainer() || edit.after().type().isArray()) {
				return true;
			}
		}
		return cause == DocumentCause.RESET;
	}
}
