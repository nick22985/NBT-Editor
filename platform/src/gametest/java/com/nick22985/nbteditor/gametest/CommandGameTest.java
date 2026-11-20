package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.data.AttributeModifier;
import com.nick22985.nbteditor.data.BookField;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.PotionContents;
import com.nick22985.nbteditor.data.PotionEffect;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.mixin.ChatScreenAccessor;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.fabric.util.McEntityUtil;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.Entity;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class CommandGameTest implements FabricClientGameTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final BlockPos PLACED = new BlockPos(4, -60, 4);
	private static final int INVENTORY = 36;
	private static final int LONG = 300;

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		GameVersion version = kernel.platform().game().version();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(100);

			this.reset(context, world);
			this.run(context, kernel, "get item diamond 3");
			CompoundTag diamond = this.find(context, kernel, "minecraft:diamond");
			if (ItemStackUtil.count(diamond) != 3) {
				throw new AssertionError("/get item gave " + diamond);
			}

			this.reset(context, world);
			String name = "A".repeat(LONG);
			context.setScreen(() -> new ChatScreen("", false));
			context.waitTicks(5);
			context.getInput().typeChars("/get snbt {id:\"minecraft:stone\",count:1,components:{\"minecraft:custom_name\":\"" + name + "\"}}");
			context.waitTicks(2);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(15);
			if (!this.find(context, kernel, "minecraft:stone").toString().contains(name)) {
				throw new AssertionError("A long /get snbt was cut short");
			}
			context.setScreen(() -> new ChatScreen("", false));
			context.waitTicks(5);
			context.getInput().typeChars(name);
			context.waitTicks(2);
			int typed = context.computeOnClient(client -> ((ChatScreenAccessor) client.gui.screen()).nbtedit$input().getValue().length());
			if (typed != LONG) {
				throw new AssertionError("Chat kept " + typed + " of " + LONG + " characters");
			}
			context.takeScreenshot("chat-long");
			context.setScreen(() -> null);

			this.reset(context, world);
			this.run(context, kernel, "get potion splash speed -duration:30 -amplifier:1 jump_boost");
			PotionContents potion = ItemFields.POTION_CONTENTS.get(this.find(context, kernel, "minecraft:splash_potion"), version);
			List<PotionEffect> effects = potion == null ? List.of() : potion.effects();
			if (effects.size() != 2 || effects.get(0).amplifier() != 1 || effects.get(0).duration() != 600) {
				throw new AssertionError("/get potion gave " + potion);
			}

			this.reset(context, world);
			this.run(context, kernel, "get soup blindness 8");
			List<PotionEffect> stew = ItemFields.STEW_EFFECTS.get(this.find(context, kernel, "minecraft:suspicious_stew"), version);
			if (stew == null || stew.size() != 1 || stew.get(0).duration() != 160) {
				throw new AssertionError("/get soup gave " + stew);
			}

			this.reset(context, world);
			world.getServer().runCommand("item replace entity @a weapon.mainhand with diamond_sword");
			context.waitTicks(5);
			this.run(context, kernel, "factory name &6Shiny Sword");
			this.run(context, kernel, "factory lore add First line");
			this.run(context, kernel, "factory lore add Second line");
			this.run(context, kernel, "factory lore remove 1");
			this.run(context, kernel, "factory unbreakable");
			this.run(context, kernel, "factory max");
			this.run(context, kernel, "factory randomuuid");
			this.run(context, kernel, "factory damage 12");
			this.run(context, kernel, "factory model 7");
			this.run(context, kernel, "factory glint false");
			CompoundTag sword = this.find(context, kernel, "minecraft:diamond_sword");
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
			Map<String, Integer> enchantments = ItemFields.ENCHANTMENTS.get(sword, version);
			if (enchantments == null || enchantments.getOrDefault("minecraft:sharpness", 0) != 5) {
				throw new AssertionError("/factory max gave " + enchantments);
			}
			if (ItemStackUtil.component(sword, "custom_data") == null) {
				throw new AssertionError("/factory randomuuid didn't add data to " + sword);
			}
			if (!Integer.valueOf(12).equals(ItemFields.DAMAGE.get(sword, version)) || !Integer.valueOf(7).equals(ItemFields.CUSTOM_MODEL_DATA.get(sword, version)) || ItemStackUtil.component(sword, "enchantment_glint_override") == null) {
				throw new AssertionError("/factory damage, model or glint didn't apply to " + sword);
			}
			this.run(context, kernel, "factory lore list");
			context.waitTicks(5);
			context.takeScreenshot("command-lore-list");
			this.run(context, kernel, "factory randomuuid remove");
			if (ItemStackUtil.component(this.find(context, kernel, "minecraft:diamond_sword"), "custom_data") != null) {
				throw new AssertionError("/factory randomuuid remove left the UUID");
			}
			this.run(context, kernel, "nbt");
			this.expectScreen(context, "EditorScreen");
			context.takeScreenshot("command-sword");
			this.close(context);

			this.reset(context, world);
			world.getServer().runCommand("item replace entity @a weapon.mainhand with iron_sword[attribute_modifiers=[{type:\"minecraft:attack_damage\",id:\"nbtedit:test\",amount:5,operation:\"add_value\",slot:\"mainhand\"}]]");
			context.waitTicks(5);
			this.run(context, kernel, "factory attributes newuuids");
			List<AttributeModifier> modifiers = ItemFields.ATTRIBUTE_MODIFIERS.get(this.find(context, kernel, "minecraft:iron_sword"), version);
			if (modifiers == null || modifiers.size() != 1 || modifiers.get(0).id().equals("nbtedit:test")) {
				throw new AssertionError("/factory attributes newuuids left " + modifiers);
			}

			this.reset(context, world);
			this.run(context, kernel, "factory book new");
			this.expectScreen(context, "EditorScreen");
			this.close(context);
			this.find(context, kernel, BookField.WRITTEN);
			this.run(context, kernel, "factory book make_writable");
			this.find(context, kernel, BookField.WRITABLE);

			this.reset(context, world);
			this.run(context, kernel, "factory sign new oak_sign");
			this.expectScreen(context, "EditorScreen");
			context.takeScreenshot("command-sign");
			this.close(context);

			this.run(context, kernel, "get block place 4 -60 4 minecraft:gold_block");
			context.waitTicks(20);
			BlockData placed = this.await(context, () -> kernel.platform().server().readBlock(OVERWORLD, PLACED));
			if (placed == null || !placed.blockId().equals("minecraft:gold_block")) {
				throw new AssertionError("/get block place left " + placed);
			}

			this.run(context, kernel, "get entity summon 6 -60 6 minecraft:cow {NoAI:1b}");
			context.waitTicks(20);
			boolean cow = world.getServer().computeOnServer(server -> {
				for (Entity entity : server.overworld().getAllEntities()) {
					if (McEntityUtil.typeId(entity).equals("minecraft:cow")) {
						return true;
					}
				}
				return false;
			});
			if (!cow) {
				throw new AssertionError("/get entity summon didn't summon a cow");
			}

			this.run(context, kernel, "clientchest 2");
			this.expectScreen(context, "ClientChestScreen");
			context.takeScreenshot("command-clientchest");
			this.close(context);

			this.run(context, kernel, "get lostitem");
			this.expectScreen(context, "LostItemScreen");
			context.takeScreenshot("command-lostitem");
			this.close(context);

			this.run(context, kernel, "get credits");
			this.run(context, kernel, "nbt help");
			context.waitTicks(10);
			context.takeScreenshot("command-chat");
			this.run(context, kernel, "nbt help open");
			this.run(context, kernel, "nbt help clientchest");
			context.waitTicks(10);
			context.takeScreenshot("command-help-topics");
			this.run(context, kernel, "get help textformat");
			context.waitTicks(10);
			context.takeScreenshot("command-help-textformat");
			this.run(context, kernel, "get help");
			this.run(context, kernel, "get help gradient");
			context.waitTicks(10);
			context.takeScreenshot("command-help-gradient");
		}
	}

	private void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("clear @a");
		context.waitTicks(5);
	}

	private void run(ClientGameTestContext context, Kernel kernel, String command) {
		context.runOnClient(client -> kernel.platform().client().commands().send(command));
		context.waitTicks(15);
	}

	private CompoundTag find(ClientGameTestContext context, Kernel kernel, String id) {
		CompoundTag found = context.computeOnClient(client -> {
			PlayerService player = kernel.platform().client().player();
			for (int slot = 0; slot < INVENTORY; slot++) {
				CompoundTag tag = player.slot(slot).tag();
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

	private void expectScreen(ClientGameTestContext context, String name) {
		String shown = context.computeOnClient(client -> {
			UiScreen ui = this.ui(client);
			return ui == null ? null : ui.getClass().getSimpleName();
		});
		if (!name.equals(shown)) {
			throw new AssertionError("Expected " + name + " but the screen was " + shown);
		}
	}

	private void close(ClientGameTestContext context) {
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private <T> T await(ClientGameTestContext context, Supplier<CompletableFuture<T>> task) {
		CompletableFuture<T> future = context.computeOnClient(client -> task.get());
		for (int i = 0; i < 400 && !future.isDone(); i++) {
			context.waitTicks(1);
		}
		if (!future.isDone()) {
			throw new AssertionError("Timed out waiting for a world operation");
		}
		return future.join();
	}

	private UiScreen ui(Minecraft client) {
		//? if >=26.3 {
		Object screen = client.gui.screen();
		//?} else
		//Object screen = client.screen;
		return screen instanceof HostScreen ? ((HostScreen) screen).ui() : null;
	}
}
