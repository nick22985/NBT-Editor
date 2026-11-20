package com.nick22985.nbteditor.smoke;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.PotionContents;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.compat.ModMenuEntry;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.module.ModuleInfo;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SmokeRun implements Runnable {
	private static final int INVENTORY = 36;
	private static final String[][] TABS = {
		{"diamond_sword", "display"}, {"diamond_sword", "enchantments"}, {"diamond_sword", "attributes"},
		{"diamond_sword", "tooltip"}, {"diamond_sword", "nbt"}, {"potion", "potion"}, {"leather_chestplate", "armor"},
		{"writable_book", "book"}, {"zombie_spawn_egg", "mob"}, {"player_head", "misc"}
	};
	private static final String[][] SCREENS = {
		{"clientchest 2", "ClientChestScreen"}, {"get lostitem", "LostItemScreen"}, {"nbt import", "ImportScreen"},
		{"nbt file", "FileBrowserScreen"}, {"nbt self", "EditorScreen"}, {"nbt gamerules", "EditorScreen"},
		{"nbt storage smoke:test", "EditorScreen"}, {"nbt config", "SettingsScreen"}
	};

	private final SmokeContext context;
	private Kernel kernel;

	public SmokeRun(SmokeContext context) {
		this.context = context;
	}

	@Override
	public void run() {
		try {
			this.context.waitFor("the game to load", 900, client -> client.getOverlay() == null && client.screen != null);
			this.context.run(client -> client.getTutorial().setStep(TutorialSteps.NONE));
			this.context.waitTicks(20);
			this.kernel = NbteRuntime.INSTANCE.kernel();
			this.context.note("minecraft " + this.kernel.platform().game().version());
			this.context.step("modules", this::modules);
			this.context.step("settings", this::settings);
			boolean[] world = {false};
			this.context.step("world", () -> {
				this.world();
				world[0] = true;
			});
			if (world[0]) {
				this.context.step("mixin audit", () -> this.context.run(client -> MixinEnvironment.getCurrentEnvironment().audit()));
				this.context.step("op and creative", this::creative);
				for (String[] tab : TABS) {
					this.context.step("editor " + tab[0] + " " + tab[1], () -> this.editor(tab[0], tab[1]));
				}
				this.context.step("get item", this::getItem);
				this.context.step("get potion", this::getPotion);
				this.context.step("factory", this::factory);
				this.context.step("block place", this::blockPlace);
				for (String[] screen : SCREENS) {
					this.context.step("/" + screen[0], () -> this.screen(screen[0], screen[1]));
				}
				this.context.step("chat", this::chat);
			}
		} catch (Throwable t) {
			this.context.step("setup", () -> {
				throw new AssertionError(t);
			});
		} finally {
			this.context.finish();
		}
	}

	private void modules() {
		if (this.kernel == null) {
			throw new AssertionError("Kernel didn't start");
		}
		for (ModuleInfo info : this.kernel.modules()) {
			if (!info.enabled() && info.module().enabledByDefault()) {
				throw new AssertionError("Module " + info.id() + " is " + info.state() + (info.reason() == null ? "" : " " + info.reason()));
			}
		}
	}

	private void settings() {
		this.context.run(client -> client.setScreen(new ModMenuEntry().getModConfigScreenFactory().create(new TitleScreen())));
		this.context.waitTicks(10);
		this.expectHost("SettingsScreen");
		this.context.screenshot("settings");
		this.context.run(client -> client.setScreen(new TitleScreen()));
		this.context.waitTicks(5);
	}

	private void world() {
		//? if >=1.19 {
		this.context.run(client -> CreateWorldScreen.openFresh(client, new TitleScreen()));
		//?} else if >=1.18 {
		/*this.context.run(client -> client.setScreen(CreateWorldScreen.createFresh(new TitleScreen())));
		*///?} else
		//this.context.run(client -> client.setScreen(CreateWorldScreen.create(new TitleScreen())));
		this.context.waitFor("the create world screen", 120, client -> client.screen instanceof CreateWorldScreen);
		this.context.waitTicks(10);
		this.context.run(client -> {
			String label = I18n.get("selectWorld.create");
			for (GuiEventListener child : client.screen.children()) {
				if (child instanceof Button && ((Button) child).getMessage().getString().equals(label)) {
					((Button) child).onPress();
					return;
				}
			}
			throw new AssertionError("No create button on " + client.screen.getClass().getName());
		});
		this.context.waitFor("the world to load", 900, client -> client.player != null && client.level != null && client.screen == null);
		this.context.waitTicks(40);
		this.context.screenshot("world");
	}

	private void creative() {
		this.context.run(client -> {
			IntegratedServer server = client.getSingleplayerServer();
			server.execute(() -> {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					server.getPlayerList().op(player.getGameProfile());
					player.setGameMode(GameType.CREATIVE);
				}
			});
		});
		this.context.waitFor("creative mode", 60, client -> this.player().creative() && this.player().permissionLevel() >= 2);
		this.context.waitTicks(10);
	}

	private void editor(String item, String tab) {
		this.hold(item);
		this.context.run(client -> this.kernel.services().get(EditorService.class).open(this.kernel.services().get(ItemTargets.class).held(Hand.MAIN)));
		this.context.waitTicks(10);
		this.context.run(client -> {
			UiScreen ui = this.ui(client);
			if (ui == null) {
				throw new AssertionError("Editor didn't open for " + item);
			}
			try {
				ui.getClass().getMethod("switchTab", String.class).invoke(ui, tab);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
		this.context.waitTicks(10);
		this.expectHost("EditorScreen");
		this.context.screenshot("editor-" + tab);
		this.close();
	}

	private void getItem() {
		this.clear();
		this.command("get item diamond 3");
		CompoundTag diamond = this.find("minecraft:diamond");
		if (ItemStackUtil.count(diamond) != 3) {
			throw new AssertionError("/get item gave " + diamond);
		}
	}

	private void getPotion() {
		this.clear();
		this.command("get potion splash speed -duration:30 -amplifier:1 jump_boost");
		PotionContents potion = ItemFields.POTION_CONTENTS.get(this.find("minecraft:splash_potion"), this.version());
		if (potion == null || potion.effects().size() != 2 || potion.effects().get(0).amplifier() != 1 || potion.effects().get(0).duration() != 600) {
			throw new AssertionError("/get potion gave " + potion);
		}
	}

	private void factory() {
		this.hold("diamond_sword");
		this.command("factory name &6Shiny Sword");
		this.command("factory lore add First line");
		this.command("factory unbreakable");
		this.command("factory max");
		CompoundTag sword = this.find("minecraft:diamond_sword");
		GameVersion version = this.version();
		if (ItemFields.CUSTOM_NAME.get(sword, version) == null) {
			throw new AssertionError("/factory name didn't name " + sword);
		}
		List<?> lore = ItemFields.LORE.get(sword, version);
		if (lore == null || lore.size() != 1) {
			throw new AssertionError("/factory lore left " + lore);
		}
		if (!Boolean.TRUE.equals(ItemFields.UNBREAKABLE.get(sword, version))) {
			throw new AssertionError("/factory unbreakable didn't apply to " + sword);
		}
		this.command("nbt");
		this.expectHost("EditorScreen");
		this.context.screenshot("factory-sword");
		this.close();
	}

	private void blockPlace() {
		BlockPos at = this.context.compute(client -> this.player().position().block());
		BlockPos target = new BlockPos(at.x() + 2, at.y(), at.z() + 2);
		this.command("get block place " + target.x() + " " + target.y() + " " + target.z() + " minecraft:gold_block");
		this.context.waitTicks(20);
		BlockData placed = this.await(this.context.compute(client -> this.kernel.platform().server().readBlock("minecraft:overworld", target)));
		if (placed == null || !placed.blockId().equals("minecraft:gold_block")) {
			throw new AssertionError("/get block place left " + placed);
		}
		this.command("nbt block " + target.x() + " " + target.y() + " " + target.z());
		this.expectHost("EditorScreen");
		this.context.screenshot("block");
		this.close();
	}

	private void chat() {
		this.command("get credits");
		this.command("nbt help");
		this.command("get help textformat");
		this.context.waitTicks(10);
		this.context.run(client -> client.setScreen(new ChatScreen("")));
		this.context.waitTicks(10);
		this.context.screenshot("chat");
		this.close();
	}

	private void screen(String command, String name) {
		this.command(command);
		this.expectHost(name);
		this.context.screenshot(command.replace(' ', '-'));
		this.close();
	}

	private void hold(String item) {
		PlayerService player = this.player();
		UUID uuid = this.context.compute(client -> player.uuid());
		int slot = this.context.compute(client -> player.selectedSlot());
		CompoundTag tag = ItemStackUtil.create("minecraft:" + item, 1, this.version());
		if (!Boolean.TRUE.equals(this.await(this.context.compute(client -> this.kernel.platform().server().setPlayerSlot(uuid, slot, tag))))) {
			throw new AssertionError("Couldn't put " + item + " in slot " + slot);
		}
		this.context.waitTicks(10);
	}

	private void clear() {
		PlayerService player = this.player();
		UUID uuid = this.context.compute(client -> player.uuid());
		for (int slot = 0; slot < INVENTORY; slot++) {
			int index = slot;
			this.await(this.context.compute(client -> this.kernel.platform().server().setPlayerSlot(uuid, index, new CompoundTag())));
		}
		this.context.waitTicks(10);
	}

	private void command(String command) {
		this.context.run(client -> this.kernel.platform().client().commands().send(command));
		this.context.waitTicks(15);
	}

	private CompoundTag find(String id) {
		CompoundTag found = this.context.compute(client -> {
			for (int slot = 0; slot < INVENTORY; slot++) {
				CompoundTag tag = this.player().slot(slot).tag();
				if (id.equals(ItemStackUtil.id(tag))) {
					return tag.copy();
				}
			}
			return null;
		});
		if (found == null) {
			throw new AssertionError("No " + id + " in the inventory");
		}
		return found;
	}

	private void expectHost(String name) {
		String shown = this.context.compute(client -> {
			UiScreen ui = this.ui(client);
			if (ui != null) {
				return ui.getClass().getSimpleName();
			}
			return client.screen == null ? null : client.screen.getClass().getName();
		});
		if (!name.equals(shown)) {
			throw new AssertionError("Expected " + name + " but the screen was " + shown);
		}
	}

	private void close() {
		this.context.run(client -> client.setScreen(null));
		this.context.waitTicks(3);
	}

	private <T> T await(CompletableFuture<T> future) {
		try {
			return future.get(60, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private PlayerService player() {
		return this.kernel.platform().client().player();
	}

	private GameVersion version() {
		return this.kernel.platform().game().version();
	}

	private UiScreen ui(Minecraft client) {
		Screen screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
