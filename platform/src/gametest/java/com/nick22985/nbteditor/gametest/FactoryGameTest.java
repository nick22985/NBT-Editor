package com.nick22985.nbteditor.gametest;

import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.platform.Hand;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.ui.UiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class FactoryGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			this.shoot(context, world, kernel, "leather_chestplate[trim={material:\"minecraft:gold\",pattern:\"minecraft:coast\"}]", "armor");
			this.shoot(context, world, kernel, "potion[potion_contents={potion:\"minecraft:swiftness\",custom_effects:[{id:\"minecraft:night_vision\",duration:-1}]}]", "potion");
			this.shoot(context, world, kernel, "suspicious_stew[suspicious_stew_effects=[{id:\"minecraft:blindness\",duration:160}]]", "potion");
			this.shoot(context, world, kernel, "oak_sign", "sign");
			this.shoot(context, world, kernel, "writable_book[writable_book_content={pages:[\"Hello there\",\"Second page\"]}]", "book");
			this.shoot(context, world, kernel, "written_book[written_book_content={title:\"Tale\",author:\"nick\",pages:[\"First page of the tale\"]}]", "book");
			this.shoot(context, world, kernel, "oak_stairs", "block_states");
			this.shoot(context, world, kernel, "player_head", "misc");
			this.shoot(context, world, kernel, "zombie_spawn_egg", "mob");
		}
	}

	private void shoot(ClientGameTestContext context, TestSingleplayerContext world, Kernel kernel, String item, String tab) {
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
				Object current = ui.getClass().getMethod("tab").invoke(ui);
				if (!tab.equals(current)) {
					throw new AssertionError("Tab " + tab + " isn't available for " + item + ", got " + current);
				}
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
		context.waitTicks(5);
		String name = item.contains("[") ? item.substring(0, item.indexOf('[')) : item;
		context.takeScreenshot("factory-" + tab + "-" + name);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
