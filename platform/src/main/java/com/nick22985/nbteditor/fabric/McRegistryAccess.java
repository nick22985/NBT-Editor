package com.nick22985.nbteditor.fabric;

import java.util.concurrent.CompletableFuture;
//? if >=1.19.3 {
import java.util.ArrayList;
import java.util.List;
//?}
import net.minecraft.client.Minecraft;
//? if >=1.19.3 {
import net.minecraft.core.LayeredRegistryAccess;
//?}
import net.minecraft.core.RegistryAccess;
//? if >=1.21.2 {
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
//?}
//? if >=26.1 {
import net.minecraft.core.component.DataComponentInitializers;
//?}
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
//?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
//? if >=1.21.2 {
import net.minecraft.tags.TagLoader;
//?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McRegistryAccess {
	private final boolean client;
	private volatile MinecraftServer server;
	private volatile CompletableFuture<RegistryAccess> defaults;
	private volatile RegistryAccess fallback;

	public McRegistryAccess(boolean client) {
		this.client = client;
	}

	public void server(MinecraftServer server) {
		this.server = server;
	}

	public MinecraftServer server() {
		return this.server;
	}

	public boolean onServerThread() {
		MinecraftServer current = this.server;
		return current != null && current.isSameThread();
	}

	public RegistryAccess get() {
		MinecraftServer current = this.server;
		if (current != null && (current.isSameThread() || !this.client)) {
			return current.registryAccess();
		}
		if (this.client) {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.getConnection() != null) {
				return mc.getConnection().registryAccess();
			}
		}
		if (current != null) {
			return current.registryAccess();
		}
		CompletableFuture<RegistryAccess> loaded = this.defaults;
		if (loaded != null && loaded.isDone() && !loaded.isCompletedExceptionally()) {
			return loaded.join();
		}
		return this.fallback();
	}

	public void preload() {
		if (this.client && this.defaults == null) {
			this.defaults = new CompletableFuture<>();
			Minecraft.getInstance().execute(() -> {
				PackRepository packs = Minecraft.getInstance().getResourcePackRepository();
				if (packs.getSelectedPacks().isEmpty()) {
					packs.reload();
				}
				Thread loader = new Thread(() -> {
					try {
						this.defaults.complete(this.loadDefaults(packs));
					} catch (RuntimeException | LinkageError e) {
						this.defaults.completeExceptionally(e);
					}
				}, "nbtedit-registries");
				loader.setDaemon(true);
				loader.start();
			});
		}
	}

	private RegistryAccess fallback() {
		RegistryAccess current = this.fallback;
		if (current == null) {
			//? if >=1.19.3 {
			current = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
			//?} else if >=1.18 {
			/*current = RegistryAccess.BUILTIN.get();
			*///?} else
			//current = RegistryAccess.builtin();
			this.fallback = current;
		}
		return current;
	}

	private RegistryAccess loadDefaults(PackRepository packs) {
		//? if >=1.19.3 {
		LayeredRegistryAccess<RegistryLayer> layers = RegistryLayer.createRegistryAccess();
		ResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, packs.openAllSelected());
		List<RegistryDataLoader.RegistryData<?>> entries = new ArrayList<>();
		entries.addAll(RegistryDataLoader.WORLD_REGISTRIES);
		entries.addAll(RegistryDataLoader.DIMENSION_REGISTRIES);
		//? if >=1.21.2 {
		List<Registry.PendingTags<?>> tags = TagLoader.loadTagsForExistingRegistries(resources, layers.getLayer(RegistryLayer.STATIC));
		RegistryAccess.Frozen preceding = layers.getAccessForLoading(RegistryLayer.RELOADABLE);
		List<HolderLookup.RegistryLookup<?>> lookups = TagLoader.buildUpdatedLookups(preceding, tags);
		//?}
		//? if >=26.1 {
		RegistryAccess loaded = RegistryDataLoader.load(resources, lookups, entries, Runnable::run).join();
		//?} else if >=1.21.2 {
		/*RegistryAccess loaded = RegistryDataLoader.load(resources, lookups, entries);
		*///?} else
		//RegistryAccess loaded = RegistryDataLoader.load(resources, layers.compositeAccess(), entries);
		RegistryAccess result = layers.replaceFrom(RegistryLayer.RELOADABLE, (RegistryAccess.Frozen) loaded).compositeAccess();
		//? if >=26.1 {
		for (DataComponentInitializers.PendingComponents<?> pending : BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(result)) {
			pending.apply();
		}
		//?}
		return result;
		//?} else
		//return this.fallback();
	}
}
