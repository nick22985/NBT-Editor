package com.nick22985.nbteditor.gametest;

import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.fabric.util.McEntityUtil;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.target.WorldBackends;
import com.nick22985.nbteditor.target.WorldTargets;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.util.TagMapUtil;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class WorldGameTest implements FabricClientGameTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final BlockPos CHEST = new BlockPos(2, -60, 2);
	private static final BlockPos BARREL = new BlockPos(3, -60, 2);
	private static final String LOOT = "minecraft:chests/simple_dungeon";
	private static final String SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjkzMDYyNDI5ZTQ5MWRlYzRjYmE2Mjk0ZTlmZjk1ZDdlNzM5ZWIwMjJkZDI5ZjgwOWVmMmMxMDQ4MjUzMzAwIn19fQ==";

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		WorldTargets targets = kernel.services().get(WorldTargets.class);
		WorldBackends backends = kernel.services().get(WorldBackends.class);
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("setblock 2 -60 2 minecraft:chest");
			world.getServer().runCommand("setblock 3 -60 2 minecraft:barrel");
			world.getServer().runCommand("summon minecraft:pig 5 -60 5 {NoAI:1b}");
			context.waitTicks(120);
			WorldBackend best = context.computeOnClient(client -> backends.best(SubjectKind.BLOCK, TargetCapability.WRITE).orElse(null));
			if (best == null || !best.id().equals("server")) {
					throw new AssertionError("Expected the server bridge, got " + best);
			}
			Target chest = targets.block(OVERWORLD, CHEST);
			CompoundTag doc = this.await(context, () -> chest.load());
			if (!doc.getString("Name", "").equals("minecraft:chest")) {
				throw new AssertionError("Chest document was " + doc);
			}
			doc.getOrCreateCompound("nbt").putString("LootTable", LOOT);
			this.await(context, () -> chest.save(doc));
			BlockData stored = this.await(context, () -> kernel.platform().server().readBlock(OVERWORLD, CHEST));
			if (stored.blockEntity() == null || !LOOT.equals(stored.blockEntity().getString("LootTable", null))) {
				throw new AssertionError("Chest loot table was not written: " + stored);
			}
			context.runOnClient(client -> kernel.services().get(EditorService.class).open(chest));
			context.waitTicks(20);
			if (context.computeOnClient(this::ui) == null) {
				throw new AssertionError("Block editor didn't open");
			}
			context.takeScreenshot("world-block");
			context.setScreen(() -> null);
			context.waitTicks(2);

			UUID pig = world.getServer().computeOnServer(server -> {
				for (Entity entity : server.overworld().getAllEntities()) {
					if (McEntityUtil.typeId(entity).equals("minecraft:pig")) {
						return entity.getUUID();
					}
				}
				return null;
			});
			if (pig == null) {
				throw new AssertionError("Pig was not summoned");
			}
			Target entity = targets.entity(OVERWORLD, pig, -1);
			CompoundTag pigDoc = this.await(context, () -> entity.load());
			pigDoc.putBoolean("NoGravity", true);
			this.await(context, () -> entity.save(pigDoc));
			CompoundTag pigStored = this.await(context, () -> kernel.platform().server().readEntity(OVERWORLD, pig));
			if (!pigStored.getBoolean("NoGravity", false)) {
				throw new AssertionError("Pig NoGravity was not written");
			}
			CompoundTag cowDoc = this.await(context, () -> entity.load());
			cowDoc.putString("id", "minecraft:cow");
			this.await(context, () -> entity.save(cowDoc));
			CompoundTag cowStored = this.await(context, () -> kernel.platform().server().readEntity(OVERWORLD, pig));
			if (cowStored == null || !"minecraft:cow".equals(cowStored.getString("id", null))) {
				throw new AssertionError("Entity was not recreated as a cow: " + cowStored);
			}
			context.runOnClient(client -> kernel.services().get(EditorService.class).open(entity));
			context.waitTicks(20);
			context.takeScreenshot("world-entity");
			context.setScreen(() -> null);
			context.waitTicks(2);

			WorldBackend vanilla = context.computeOnClient(client -> backends.all().stream().filter(backend -> backend.id().equals("vanilla")).findFirst().orElse(null));
			if (vanilla == null || !context.computeOnClient(client -> vanilla.available())) {
				throw new AssertionError("Vanilla bridge is not available");
			}
			BlockData barrel = this.await(context, () -> vanilla.readBlock(OVERWORLD, BARREL));
			if (barrel == null || barrel.blockEntity() == null) {
				throw new AssertionError("Vanilla read of the barrel failed: " + barrel);
			}
			CompoundTag barrelNbt = barrel.blockEntity().copy();
			barrelNbt.putString("LootTable", LOOT);
			this.await(context, () -> vanilla.writeBlock(OVERWORLD, BARREL, new BlockData(barrel.blockId(), barrel.properties(), barrelNbt), null));
			BlockData barrelStored = this.await(context, () -> kernel.platform().server().readBlock(OVERWORLD, BARREL));
			if (!LOOT.equals(barrelStored.blockEntity().getString("LootTable", null))) {
				throw new AssertionError("Vanilla write of the barrel failed: " + barrelStored);
			}
			CompoundTag rules = this.await(context, () -> vanilla.gameRules().thenApply(TagMapUtil::fromStrings));
			if (rules.size() < 10) {
				throw new AssertionError("Game rules were not read: " + rules);
			}
			CompoundTag stored2 = this.await(context, () -> vanilla.readEntity(OVERWORLD, pig));
			if (!stored2.getBoolean("NoGravity", false)) {
				throw new AssertionError("Vanilla read of the cow failed: " + stored2);
			}
			stored2.putBoolean("NoGravity", false);
			this.await(context, () -> vanilla.writeEntity(OVERWORLD, pig, stored2, false));
			CompoundTag stored3 = this.await(context, () -> kernel.platform().server().readEntity(OVERWORLD, pig));
			if (stored3.getBoolean("NoGravity", false)) {
				throw new AssertionError("Vanilla write of the cow failed: " + stored3);
			}
			CompoundTag storage = new CompoundTag().putString("greeting", "hello").put("nested", new CompoundTag().putInt("n", 3));
			this.await(context, () -> vanilla.writeStorage("nbte:test", storage));
			CompoundTag storageRead = this.await(context, () -> vanilla.readStorage("nbte:test"));
			if (!storage.equals(storageRead)) {
				throw new AssertionError("Vanilla storage round trip failed: " + storageRead);
			}
			String rule = rules.contains("doDaylightCycle") ? "doDaylightCycle" : "advance_time";
			String flipped = "true".equals(rules.getString(rule, null)) ? "false" : "true";
			this.await(context, () -> vanilla.setGameRule(rule, flipped));
			CompoundTag bridged = this.await(context, () -> targets.gameRules().load());
			if (!flipped.equals(bridged.getString(rule, null))) {
				throw new AssertionError("Game rule " + rule + " was not set: " + bridged);
			}

			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("setblock 4 -60 2 minecraft:player_head{profile:{id:[I;1,2,3,4],properties:[{name:\"textures\",value:\"" + SKIN + "\"}]}}");
			context.waitTicks(20);
			context.runOnClient(client -> client.gameMode.handlePickItemFromBlock(new net.minecraft.core.BlockPos(4, -60, 2), false));
			context.waitTicks(20);
			if (context.computeOnClient(client -> client.player.getMainHandItem().get(DataComponents.PROFILE)) == null) {
				throw new AssertionError("Picking the head lost its profile: " + context.computeOnClient(client -> client.player.getMainHandItem().getComponents()));
			}
			boolean synced = world.getServer().computeOnServer(server -> {
				for (ItemStack stack : server.getPlayerList().getPlayers().get(0).getInventory().getNonEquipmentItems()) {
					if (stack.is(Items.PLAYER_HEAD) && stack.get(DataComponents.PROFILE) != null) {
						return true;
					}
				}
				return false;
			});
			if (!synced) {
				throw new AssertionError("The picked head didn't reach the server inventory");
			}
		}
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
