package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.config.ConfigStore;
import com.nick22985.nbteditor.fabric.slot.SlotGuard;
import com.nick22985.nbteditor.fabric.net.FabricTransport;
import com.nick22985.nbteditor.fabric.server.FabricServerPlatform;
import com.nick22985.nbteditor.hook.Hooks;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.net.NetService;
import com.nick22985.nbteditor.platform.ClientPlatform;
import com.nick22985.nbteditor.platform.PickListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbteRuntime {
	public static final NbteRuntime INSTANCE = new NbteRuntime();
	public static final String MODULE_ENTRYPOINT = FabricGameInfo.MOD_ID + ":module";
	private static final String MODULE_INDEX = FabricGameInfo.MOD_ID + "/modules.index";

	private final FabricGameInfo game = new FabricGameInfo();
	private final FabricLog log = new FabricLog("NBT Editor");
	private final McRegistryAccess registries = new McRegistryAccess(this.game.isClient());
	private final FabricScheduler scheduler = new FabricScheduler(this.registries, this.game.isClient(), this.log.child("scheduler"));
	private final FabricTransport transport = new FabricTransport(this.registries::server);
	private final Hooks hooks = new Hooks(this.log.child("hooks"));
	private final NetService net = new NetService(this.transport, this.log.child("net"));
	private final SlotGuard slots = new SlotGuard(this.hooks);
	private BiPredicate<Component, Boolean> chatHandler = (message, overlay) -> false;
	private BiConsumer<Screen, List<Path>> dropHandler = (screen, paths) -> {
	};
	private BiPredicate<Screen, Slot> clickHandler = (screen, slot) -> false;
	private BooleanSupplier longChat = () -> false;
	private Predicate<String> clientCommand = command -> false;
	private PickListener pickListener;
	private boolean commonReady;
	private Kernel kernel;

	private NbteRuntime() {
	}

	public McRegistryAccess registries() {
		return this.registries;
	}

	public FabricGameInfo game() {
		return this.game;
	}

	public FabricLog log() {
		return this.log;
	}

	public FabricScheduler scheduler() {
		return this.scheduler;
	}

	public FabricTransport transport() {
		return this.transport;
	}

	public Hooks hooks() {
		return this.hooks;
	}

	public NetService net() {
		return this.net;
	}

	public SlotGuard slots() {
		return this.slots;
	}

	public Kernel kernel() {
		return this.kernel;
	}

	public void chatHandler(BiPredicate<Component, Boolean> handler) {
		this.chatHandler = handler;
	}

	public boolean chatReceived(Component message, boolean overlay) {
		try {
			return this.chatHandler.test(message, overlay);
		} catch (RuntimeException e) {
			this.log.error("Chat hook failed", e);
			return false;
		}
	}

	public void dropHandler(BiConsumer<Screen, List<Path>> handler) {
		this.dropHandler = handler;
	}

	public void filesDropped(Screen screen, List<Path> paths) {
		try {
			this.dropHandler.accept(screen, paths);
		} catch (RuntimeException e) {
			this.log.error("File drop hook failed", e);
		}
	}

	public void clickHandler(BiPredicate<Screen, Slot> handler) {
		this.clickHandler = handler;
	}

	public boolean slotClicked(Screen screen, Slot slot) {
		try {
			return this.clickHandler.test(screen, slot);
		} catch (RuntimeException e) {
			this.log.error("Slot click hook failed", e);
			return false;
		}
	}

	public void longChat(BooleanSupplier enabled) {
		this.longChat = enabled;
	}

	public boolean longChat() {
		return this.longChat.getAsBoolean();
	}

	public void pickListener(PickListener listener) {
		this.pickListener = listener;
	}

	public PickListener pickListener() {
		return this.pickListener;
	}

	public void clientCommands(Predicate<String> handles) {
		this.clientCommand = handles;
	}

	public boolean clientCommand(String message) {
		return message.startsWith("/") && this.clientCommand.test(message.substring(1));
	}

	public void initCommon() {
		if (this.commonReady) {
			return;
		}
		this.commonReady = true;
		this.transport.registerCommon();
		new FabricServerHooks(() -> this.hooks, () -> this.net, this.registries, this.scheduler, !this.game.isClient()).register();
	}

	public void start(ClientPlatform client) {
		if (this.kernel != null) {
			return;
		}
		FabricPlatform platform = new FabricPlatform(this.game, this.log, this.scheduler, this.transport, client, new FabricServerPlatform(this.registries));
		ConfigStore config = new ConfigStore(this.game.configDir().resolve("config.json"), this.log.child("config"));
		config.load();
		this.kernel = new Kernel(platform, config, this.hooks, this.net);
		this.kernel.start(this.discover());
	}

	public void stop() {
		if (this.kernel != null) {
			this.kernel.stop();
		}
	}

	private List<Module> discover() {
		Set<String> names = new LinkedHashSet<>();
		try {
			Enumeration<URL> indexes = NbteRuntime.class.getClassLoader().getResources(MODULE_INDEX);
			while (indexes.hasMoreElements()) {
				this.readIndex(indexes.nextElement(), names);
			}
		} catch (IOException e) {
			this.log.error("Could not list module indexes", e);
		}
		List<Module> modules = new ArrayList<>();
		for (String name : names) {
			try {
				Class<?> type = Class.forName(name, true, NbteRuntime.class.getClassLoader());
				modules.add((Module) type.getDeclaredConstructor().newInstance());
			} catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
				this.log.error("Could not load module " + name, e);
			}
		}
		for (Module module : FabricLoader.getInstance().getEntrypoints(MODULE_ENTRYPOINT, Module.class)) {
			modules.add(module);
		}
		return modules;
	}

	private void readIndex(URL url, Set<String> names) {
		try (InputStream in = url.openStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					names.add(trimmed);
				}
			}
		} catch (IOException e) {
			this.log.error("Could not read module index " + url, e);
		}
	}
}
