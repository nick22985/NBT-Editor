package com.nick22985.nbteditor.module;

import com.nick22985.nbteditor.config.ConfigSection;
import com.nick22985.nbteditor.hook.Hooks;
import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.net.NetService;
import com.nick22985.nbteditor.platform.ClientPlatform;
import com.nick22985.nbteditor.platform.Platform;
import com.nick22985.nbteditor.platform.ServerPlatform;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModuleContext {
	private final Kernel kernel;
	private final Module module;
	private final Log log;
	private ConfigSection config;

	ModuleContext(Kernel kernel, Module module) {
		this.kernel = kernel;
		this.module = module;
		this.log = kernel.platform().log().child(module.id());
	}

	public String moduleId() {
		return this.module.id();
	}

	public Module module() {
		return this.module;
	}

	public Kernel kernel() {
		return this.kernel;
	}

	public Platform platform() {
		return this.kernel.platform();
	}

	public boolean isClient() {
		return this.kernel.platform().game().isClient();
	}

	public ClientPlatform client() {
		ClientPlatform client = this.kernel.platform().client();
		if (client == null) {
			throw new IllegalStateException("Module " + this.module.id() + " asked for the client platform on a dedicated server");
		}
		return client;
	}

	public ServerPlatform server() {
		return this.kernel.platform().server();
	}

	public Hooks hooks() {
		return this.kernel.hooks();
	}

	public <T> Registry<T> registry(RegistryKey<T> key) {
		return this.kernel.registries().get(key);
	}

	public <T> T register(RegistryKey<T> key, String id, T value) {
		return this.register(key, id, value, 1000);
	}

	public <T> T register(RegistryKey<T> key, String id, T value, int order) {
		return this.kernel.registries().get(key).register(this.module.id(), id, value, order);
	}

	public Services services() {
		return this.kernel.services();
	}

	public ConfigSection config() {
		if (this.config == null) {
			this.config = this.kernel.config().section(this.module.id());
		}
		return this.config;
	}

	public Log log() {
		return this.log;
	}

	public NetService net() {
		return this.kernel.net();
	}

	public boolean isEnabled(String moduleId) {
		return this.kernel.isEnabled(moduleId);
	}

	public String key(String suffix) {
		return "nbtedit." + this.module.id() + "." + suffix;
	}

	public Text tr(String suffix, Object... args) {
		return Text.translatable(this.key(suffix), args);
	}
}
