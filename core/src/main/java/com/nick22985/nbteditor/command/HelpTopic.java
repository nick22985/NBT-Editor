package com.nick22985.nbteditor.command;

import com.nick22985.nbteditor.module.RegistryKey;
import com.nick22985.nbteditor.text.Text;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record HelpTopic(String group, String name, Text description, Supplier<List<Text>> lines) {
	public static final RegistryKey<HelpTopic> REGISTRY = RegistryKey.of("help_topic", HelpTopic.class);

	public String id() {
		return this.group + " " + this.name;
	}
}
