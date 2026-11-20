package com.nick22985.nbteditor.module;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record RegistryKey<T>(String name, Class<T> type) {
	public static <T> RegistryKey<T> of(String name, Class<T> type) {
		return new RegistryKey<>(name, type);
	}
}
