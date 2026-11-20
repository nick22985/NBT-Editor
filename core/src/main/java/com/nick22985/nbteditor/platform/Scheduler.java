package com.nick22985.nbteditor.platform;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Scheduler {
	void client(Runnable task);

	void server(Runnable task);

	void later(int ticks, Runnable task);

	<T> CompletableFuture<T> async(Supplier<T> task);
}
