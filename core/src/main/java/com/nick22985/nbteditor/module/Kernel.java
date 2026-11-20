package com.nick22985.nbteditor.module;

import com.nick22985.nbteditor.config.ConfigStore;
import com.nick22985.nbteditor.hook.Hooks;
import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.net.NetService;
import com.nick22985.nbteditor.platform.Platform;
import com.nick22985.nbteditor.target.WorldBackends;
import com.nick22985.nbteditor.util.ModuleSortUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class Kernel {
	private final Platform platform;
	private final ConfigStore config;
	private final Hooks hooks;
	private final NetService net;
	private final Registries registries = new Registries();
	private final Services services = new Services();
	private final Map<String, ModuleInfo> modules = new LinkedHashMap<>();
	private final Map<String, ModuleContext> contexts = new LinkedHashMap<>();
	private final Log log;
	private boolean started;

	public Kernel(Platform platform, ConfigStore config, Hooks hooks, NetService net) {
		this.platform = platform;
		this.config = config;
		this.hooks = hooks;
		this.net = net;
		this.log = platform.log().child("kernel");
		this.services.provide(WorldBackends.class, new WorldBackends());
	}

	public Platform platform() {
		return this.platform;
	}

	public ConfigStore config() {
		return this.config;
	}

	public Hooks hooks() {
		return this.hooks;
	}

	public NetService net() {
		return this.net;
	}

	public Registries registries() {
		return this.registries;
	}

	public Services services() {
		return this.services;
	}

	public Collection<ModuleInfo> modules() {
		return Collections.unmodifiableCollection(this.modules.values());
	}

	public Optional<ModuleInfo> module(String id) {
		return Optional.ofNullable(this.modules.get(id));
	}

	public boolean isEnabled(String id) {
		ModuleInfo info = this.modules.get(id);
		return info != null && info.enabled();
	}

	public void start(List<Module> discovered) {
		if (this.started) {
			throw new IllegalStateException("Kernel already started");
		}
		this.started = true;
		boolean client = this.platform.game().isClient();
		Map<String, Module> byId = new LinkedHashMap<>();
		for (Module module : discovered) {
			if (byId.containsKey(module.id())) {
				this.log.warn("Duplicate module id " + module.id() + " from " + module.getClass().getName() + ", ignoring it");
				continue;
			}
			byId.put(module.id(), module);
		}
		Map<String, ModuleInfo> resolved = ModuleSortUtil.resolve(byId, module -> {
			if (module.clientOnly() && !client) {
				return ModuleState.SKIPPED;
			}
			if (module.canDisable() && !this.config.moduleEnabled(module.id(), module.enabledByDefault())) {
				return ModuleState.DISABLED;
			}
			return ModuleState.ENABLED;
		});
		for (ModuleInfo info : resolved.values()) {
			if (!info.enabled()) {
				this.modules.put(info.id(), info);
				if (info.state() != ModuleState.DISABLED && info.state() != ModuleState.SKIPPED) {
					this.log.warn("Module " + info.id() + " not loaded: " + info.reason());
				}
				continue;
			}
			ModuleContext ctx = new ModuleContext(this, info.module());
			this.modules.put(info.id(), info);
			try {
				info.module().init(ctx);
				this.contexts.put(info.id(), ctx);
			} catch (RuntimeException | LinkageError e) {
				this.log.error("Module " + info.id() + " failed to start", e);
				this.modules.put(info.id(), new ModuleInfo(info.module(), ModuleState.FAILED, String.valueOf(e.getMessage())));
			}
		}
		for (Map.Entry<String, ModuleContext> entry : this.contexts.entrySet()) {
			try {
				this.modules.get(entry.getKey()).module().ready(entry.getValue());
			} catch (RuntimeException | LinkageError e) {
				this.log.error("Module " + entry.getKey() + " failed after start", e);
			}
		}
		this.config.saveIfDirty();
		this.log.info("Started " + this.contexts.size() + " of " + this.modules.size() + " modules");
	}

	public void stop() {
		List<String> ids = new ArrayList<>(this.contexts.keySet());
		Collections.reverse(ids);
		for (String id : ids) {
			try {
				this.modules.get(id).module().shutdown(this.contexts.get(id));
			} catch (RuntimeException e) {
				this.log.error("Module " + id + " failed to stop", e);
			}
		}
		this.config.saveIfDirty();
	}

	public void setEnabled(String id, boolean enabled) {
		this.config.setModuleEnabled(id, enabled);
		this.config.save();
	}
}
