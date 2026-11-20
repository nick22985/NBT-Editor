package com.nick22985.nbteditor.hook;

import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record HookListener<E>(int priority, Consumer<E> listener) {
}
