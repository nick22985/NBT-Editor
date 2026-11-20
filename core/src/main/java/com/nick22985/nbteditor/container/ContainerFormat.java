package com.nick22985.nbteditor.container;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ContainerFormat {
	RegistryKey<ContainerFormat> REGISTRY = RegistryKey.of("container_format", ContainerFormat.class);

	boolean supports(ContainerSubject subject);

	ContainerLayout layout(ContainerSubject subject);

	List<CompoundTag> read(ContainerSubject subject);

	void write(ContainerSubject subject, List<CompoundTag> items);
}
