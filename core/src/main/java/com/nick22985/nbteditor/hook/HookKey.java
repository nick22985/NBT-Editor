package com.nick22985.nbteditor.hook;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HookKey<E> {
	private final String name;

	private HookKey(String name) {
		this.name = name;
	}

	public static <E> HookKey<E> of(String name) {
		return new HookKey<>(name);
	}

	public String name() {
		return this.name;
	}

	@Override
	public String toString() {
		return "HookKey[" + this.name + "]";
	}
}
