package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.platform.Scheduler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricScheduler implements Scheduler {
	private final McRegistryAccess registries;
	private final boolean client;
	private final Log log;
	private final List<DelayedTask> pending = new ArrayList<>();
	private final List<DelayedTask> added = new ArrayList<>();
	private final ExecutorService async = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "nbtedit-worker");
		thread.setDaemon(true);
		return thread;
	});

	public FabricScheduler(McRegistryAccess registries, boolean client, Log log) {
		this.registries = registries;
		this.client = client;
		this.log = log;
	}

	@Override
	public void client(Runnable task) {
		if (this.client) {
			Minecraft.getInstance().execute(task);
		} else {
			this.server(task);
		}
	}

	@Override
	public void server(Runnable task) {
		MinecraftServer server = this.registries.server();
		if (server != null) {
			server.execute(task);
		} else if (this.client) {
			Minecraft.getInstance().execute(task);
		} else {
			task.run();
		}
	}

	@Override
	public void later(int ticks, Runnable task) {
		synchronized (this.added) {
			this.added.add(new DelayedTask(Math.max(1, ticks), task));
		}
	}

	@Override
	public <T> CompletableFuture<T> async(Supplier<T> task) {
		return CompletableFuture.supplyAsync(task, this.async);
	}

	public void tick() {
		synchronized (this.added) {
			this.pending.addAll(this.added);
			this.added.clear();
		}
		Iterator<DelayedTask> iterator = this.pending.iterator();
		while (iterator.hasNext()) {
			DelayedTask task = iterator.next();
			if (task.tick()) {
				iterator.remove();
				try {
					task.task().run();
				} catch (RuntimeException e) {
					this.log.error("Scheduled task failed", e);
				}
			}
		}
	}
}
