package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetOpener;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface EditorService extends TargetOpener {
	String FACTORY_TAB = "display";

	void open(Target target, String tab);

	@Override
	default void open(Target target) {
		this.open(target, null);
	}
}
