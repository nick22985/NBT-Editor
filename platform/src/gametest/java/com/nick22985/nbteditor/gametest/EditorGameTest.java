package com.nick22985.nbteditor.gametest;

import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.suggest.SuggestionPopup;
import com.nick22985.nbteditor.ui.widget.TextField;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class EditorGameTest implements FabricClientGameTest {
	private static final Modifiers CONTROL = Modifiers.of(false, true, false);
	private static final Modifiers SHIFT = Modifiers.of(true, false, false);

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			//? if >=1.21.5 {
			world.getServer().runCommand("give @a diamond_sword[custom_name={text:\"Test Blade\",color:\"gold\",italic:false},enchantments={sharpness:5,unbreaking:3},lore=[\"First line\",{text:\"Second\",color:\"aqua\"}]] 1");
			//?} else
			//world.getServer().runCommand("give @a diamond_sword[custom_name='{\"text\":\"Test Blade\",\"color\":\"gold\",\"italic\":false}',enchantments={levels:{sharpness:5,unbreaking:3}},lore=['\"First line\"','{\"text\":\"Second\",\"color\":\"aqua\"}']] 1");
			context.waitTicks(10);
			context.runOnClient(client -> kernel.services().get(EditorService.class).open(kernel.services().get(ItemTargets.class).held(Hand.MAIN)));
			context.waitTicks(10);
			UiScreen editor = context.computeOnClient(this::ui);
			if (editor == null || !editor.getClass().getSimpleName().equals("EditorScreen")) {
				throw new AssertionError("Editor didn't open, got " + editor);
			}
			context.takeScreenshot("editor-nbt");
			this.key(context, Key.DOWN, Modifiers.NONE);
			this.key(context, Key.RIGHT, SHIFT);
			this.key(context, Key.DOWN, Modifiers.NONE);
			this.key(context, Key.DOWN, Modifiers.NONE);
			context.waitTicks(2);
			context.takeScreenshot("editor-expanded");
			this.key(context, Key.E, CONTROL);
			context.waitTicks(2);
			context.takeScreenshot("editor-text");
			this.key(context, Key.F, CONTROL);
			this.type(context, "aqua");
			this.key(context, Key.ENTER, Modifiers.NONE);
			context.waitTicks(2);
			String found = context.computeOnClient(client -> {
				UiScreen ui = this.ui(client);
				if (ui == null || !(ui.focusedWidget() instanceof TextField)) {
					return null;
				}
				TextField field = (TextField) ui.focusedWidget();
				return field.error() == null ? field.value() : "error";
			});
			if (!"aqua".equals(found)) {
				throw new AssertionError("Find in edit-as-text gave " + found);
			}
			context.takeScreenshot("editor-find");
			context.runOnClient(client -> this.ui(client).closeAllOverlays());
			context.waitTicks(2);
			this.key(context, Key.F, CONTROL);
			this.type(context, "sharp");
			context.waitTicks(2);
			context.takeScreenshot("editor-search");
			this.key(context, Key.ESCAPE, Modifiers.NONE);
			this.key(context, Key.ESCAPE, Modifiers.NONE);
			context.waitTicks(2);
			context.setScreen(() -> null);
			context.waitTicks(2);
			this.suggestions(context, kernel);
		}
	}

	private void suggestions(ClientGameTestContext context, Kernel kernel) {
		context.runOnClient(client -> kernel.services().get(EditorService.class).open(kernel.services().get(ItemTargets.class).held(Hand.MAIN)));
		context.waitTicks(10);
		this.key(context, Key.N, CONTROL);
		this.key(context, Key.DOWN, Modifiers.NONE);
		this.key(context, Key.ENTER, Modifiers.NONE);
		context.waitTicks(2);
		this.type(context, "cust");
		context.waitTicks(3);
		context.takeScreenshot("editor-suggest-key");
		String popup = context.computeOnClient(client -> {
			UiScreen ui = this.ui(client);
			return ui == null || !(ui.popup() instanceof SuggestionPopup) ? null : ((SuggestionPopup) ui.popup()).entries().toString();
		});
		if (popup == null || !popup.contains("minecraft:custom_data") || popup.contains("minecraft:custom_name")) {
			throw new AssertionError("Unexpected key suggestions " + popup);
		}
		this.key(context, Key.TAB, Modifiers.NONE);
		context.waitTicks(2);
		String value = context.computeOnClient(client -> {
			UiScreen ui = this.ui(client);
			return ui != null && ui.focusedWidget() instanceof TextField ? ((TextField) ui.focusedWidget()).value() : null;
		});
		if (!"minecraft:custom_data".equals(value)) {
			throw new AssertionError("Accepting a suggestion gave " + value);
		}
		this.key(context, Key.ESCAPE, Modifiers.NONE);
		this.key(context, Key.ESCAPE, Modifiers.NONE);
		context.waitTicks(2);
		context.setScreen(() -> null);
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}

	private void key(ClientGameTestContext context, int key, Modifiers modifiers) {
		context.runOnClient(client -> {
			UiScreen ui = this.ui(client);
			if (ui != null) {
				ui.dispatchKey(new KeyInput(key, 0, modifiers));
			}
		});
		context.waitTicks(1);
	}

	private void type(ClientGameTestContext context, String text) {
		context.runOnClient(client -> {
			UiScreen ui = this.ui(client);
			if (ui != null) {
				text.codePoints().forEach(codepoint -> ui.dispatchChar(codepoint, Modifiers.NONE));
			}
		});
		context.waitTicks(1);
	}
}
