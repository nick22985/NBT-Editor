package com.nick22985.nbteditor.gametest;

import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.compat.ModMenuEntry;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.RichToolbar;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class LocaleGameTest implements FabricClientGameTest {
	private static final String[] LOCALES = {"de_de", "es_es", "fr_fr", "it_it", "ja_jp", "ko_kr", "pl_pl", "pt_br", "ru_ru", "zh_cn"};

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		try {
			for (String locale : LOCALES) {
				this.language(context, locale);
				context.setScreen(() -> new ModMenuEntry().getModConfigScreenFactory().create(null));
				context.waitTicks(5);
				context.takeScreenshot("locale-" + locale + "-settings");
				context.setScreen(() -> null);
				try (TestSingleplayerContext world = context.worldBuilder().create()) {
					context.waitTicks(20);
					world.getServer().runCommand("gamemode creative @a");
					this.shoot(context, world, kernel, locale, "diamond_sword", "display");
					this.shoot(context, world, kernel, locale, "diamond_sword", "enchantments");
					this.shoot(context, world, kernel, locale, "diamond_sword", "attributes");
					this.shoot(context, world, kernel, locale, "diamond_sword", "tooltip");
					this.shoot(context, world, kernel, locale, "diamond_sword", "nbt");
					this.shoot(context, world, kernel, locale, "potion", "potion");
					this.shoot(context, world, kernel, locale, "leather_chestplate", "armor");
					this.shoot(context, world, kernel, locale, "writable_book", "book");
					this.shoot(context, world, kernel, locale, "zombie_spawn_egg", "mob");
					this.shoot(context, world, kernel, locale, "player_head", "misc");
				}
			}
		} finally {
			this.language(context, "en_us");
		}
	}

	private void language(ClientGameTestContext context, String locale) {
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			client.options.languageCode = locale;
			client.getLanguageManager().setSelected(locale);
			return client.reloadResourcePacks();
		});
		for (int i = 0; i < 600 && !reload.isDone(); i++) {
			context.waitTicks(1);
		}
		context.waitTicks(60);
	}

	private void shoot(ClientGameTestContext context, TestSingleplayerContext world, Kernel kernel, String locale, String item, String tab) {
		world.getServer().runCommand("item replace entity @a weapon.mainhand with " + item);
		context.waitTicks(5);
		context.runOnClient(client -> kernel.services().get(EditorService.class).open(kernel.services().get(ItemTargets.class).held(Hand.MAIN)));
		context.waitTicks(10);
		context.runOnClient(client -> {
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
		context.waitTicks(5);
		context.takeScreenshot("locale-" + locale + "-" + tab);
		if (tab.equals("display")) {
			boolean opened = context.computeOnClient(client -> {
				UiScreen ui = this.ui(client);
				Button more = this.toolbarMore(ui);
				if (more == null) {
					return false;
				}
				more.press();
				return ui.topOverlay() != null;
			});
			if (opened) {
				context.waitTicks(5);
				context.takeScreenshot("locale-" + locale + "-display-more");
			}
		}
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private Button toolbarMore(Widget widget) {
		if (widget instanceof RichToolbar) {
			for (Widget child : ((RichToolbar) widget).children()) {
				if (child instanceof Button && child.visible() && ((Button) child).label().plain().equals("…")) {
					return (Button) child;
				}
			}
			return null;
		}
		if (widget instanceof WidgetGroup) {
			for (Widget child : ((WidgetGroup) widget).children()) {
				Button found = this.toolbarMore(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
