package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.text.Text;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface TargetSource {
	RegistryKey<TargetSource> REGISTRY = RegistryKey.of("target_source", TargetSource.class);

	String id();

	Text name();

	SubjectKind kind();

	boolean available();

	List<Target> targets();
}
