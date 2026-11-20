package com.nick22985.nbteditor.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.ui.HostScreen;
import com.nick22985.nbteditor.fabric.util.McEntityUtil;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class FileGameTest implements FabricClientGameTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final int INVENTORY = 36;
	private static final int REGION_SIDE = 32;

	@Override
	public void runTest(ClientGameTestContext context) {
		Kernel kernel = NbteRuntime.INSTANCE.kernel();
		GameVersion version = kernel.platform().game().version();
		Path exports = kernel.platform().game().gameDir().resolve("nbtedit").resolve("exports");
		Path drops = kernel.platform().game().gameDir().resolve("nbtedit-gametest-drops");
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
			context.waitTicks(20);
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(100);
			this.clean(exports);
			this.clean(drops);

			this.reset(context, world);
			world.getServer().runCommand("item replace entity @a weapon.mainhand with diamond_sword");
			context.waitTicks(5);
			this.run(context, kernel, "factory name Export Me");
			this.run(context, kernel, "nbt export file sword");
			context.waitTicks(20);
			Path exported = this.single(exports, "sword_");
			CompoundTag file = this.read(exported);
			if (!"item".equals(file.getString("type", "")) || file.getInt("DataVersion", 0) <= 0 || !"minecraft:diamond_sword".equals(file.getString("id", ""))) {
				throw new AssertionError("/nbt export file wrote " + file);
			}
			this.run(context, kernel, "nbt export cmd");
			String command = context.computeOnClient(client -> kernel.platform().client().system().clipboard());
			if (command == null || !command.startsWith("/give @p minecraft:diamond_sword")) {
				throw new AssertionError("/nbt export cmd copied " + command);
			}
			this.run(context, kernel, "nbt export json");
			String json = context.computeOnClient(client -> kernel.platform().client().system().clipboard());
			if (json == null || !json.trim().startsWith("{") || !json.contains("diamond_sword")) {
				throw new AssertionError("/nbt export json copied " + json);
			}
			this.reset(context, world);
			world.getServer().runCommand("item replace entity @a weapon.mainhand with stone");
			context.waitTicks(5);
			this.run(context, kernel, "nbt export cmdblock");
			this.find(context, kernel, "minecraft:command_block");

			this.reset(context, world);
			this.run(context, kernel, "nbt import");
			this.expectScreen(context, "ImportScreen");
			this.drop(context, exported);
			CompoundTag imported = this.find(context, kernel, "minecraft:diamond_sword");
			if (ItemFields.CUSTOM_NAME.get(imported, version) == null) {
				throw new AssertionError("The imported sword lost its name: " + imported);
			}

			this.reset(context, world);
			Path legacy = drops.resolve("legacy.nbt");
			this.write(legacy, "{type:\"item\",DataVersion:3700,id:\"minecraft:diamond_sword\",Count:1b,tag:{Damage:5,display:{Name:'{\"text\":\"Old\"}'}}}");
			this.drop(context, legacy);
			CompoundTag upgraded = this.find(context, kernel, "minecraft:diamond_sword");
			Integer damage = ItemFields.DAMAGE.get(upgraded, version);
			if (damage == null || damage != 5 || ItemFields.CUSTOM_NAME.get(upgraded, version) == null) {
				throw new AssertionError("The 1.20.4 export didn't upgrade: " + upgraded);
			}

			this.reset(context, world);
			Path image = drops.resolve("sunset.png");
			this.writeImage(image);
			this.drop(context, image);
			CompoundTag painting = this.find(context, kernel, "minecraft:painting");
			List<?> lore = ItemFields.LORE.get(painting, version);
			if (lore == null || lore.isEmpty()) {
				throw new AssertionError("The image import gave no lore: " + painting);
			}

			BlockPos feet = context.computeOnClient(client -> kernel.platform().client().player().position().block());
			Path stairs = drops.resolve("stairs.nbt");
			this.write(stairs, "{type:\"block\",DataVersion:" + version.dataVersion() + ",id:\"minecraft:oak_stairs\",state:{facing:\"east\"},tag:{}}");
			this.drop(context, stairs);
			BlockData placed = this.await(context, () -> kernel.platform().server().readBlock(OVERWORLD, feet));
			if (placed == null || !placed.blockId().equals("minecraft:oak_stairs") || !"east".equals(placed.properties().get("facing"))) {
				throw new AssertionError("The block import placed " + placed);
			}
			Path pig = drops.resolve("pig.nbt");
			this.write(pig, "{type:\"entity\",DataVersion:" + version.dataVersion() + ",id:\"minecraft:pig\",tag:{NoAI:1b,CustomName:\"Imported\"}}");
			this.drop(context, pig);
			context.waitTicks(20);
			boolean summoned = world.getServer().computeOnServer(server -> {
				for (Entity entity : server.overworld().getAllEntities()) {
					if (McEntityUtil.typeId(entity).equals("minecraft:pig")) {
						return true;
					}
				}
				return false;
			});
			if (!summoned) {
				throw new AssertionError("The entity import didn't summon a pig");
			}
			context.takeScreenshot("files-import");
			this.close(context);

			this.run(context, kernel, "nbt file " + exports.toAbsolutePath());
			this.expectScreen(context, "FileBrowserScreen");
			context.takeScreenshot("files-browser");
			this.close(context);

			this.run(context, kernel, "nbt file " + legacy.toAbsolutePath());
			this.expectScreen(context, "EditorScreen");
			context.takeScreenshot("files-editor");
			this.close(context);

			world.getServer().runCommand("save-all flush");
			context.waitTicks(40);
			Path worldDir = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.ROOT));
			Path region = this.region(worldDir);
			this.run(context, kernel, "nbt file " + region.toAbsolutePath());
			this.expectScreen(context, "RegionScreen");
			context.waitTicks(20);
			context.takeScreenshot("files-region");
			this.clickChunk(context, 0);
			this.expectScreen(context, "EditorScreen");
			context.takeScreenshot("files-region-chunk");
			this.close(context);
		}
	}

	private void clean(Path dir) {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> files = Files.list(dir)) {
			for (Path path : (Iterable<Path>) files::iterator) {
				Files.deleteIfExists(path);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path single(Path dir, String prefix) {
		try (Stream<Path> files = Files.list(dir)) {
			Optional<Path> found = files.filter(path -> path.getFileName().toString().startsWith(prefix)).findFirst();
			if (found.isEmpty()) {
				throw new AssertionError("No " + prefix + " export in " + dir);
			}
			return found.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path region(Path worldDir) {
		try (Stream<Path> files = Files.walk(worldDir)) {
			Optional<Path> found = files.filter(path -> path.getFileName().toString().equals("r.0.0.mca") && path.getParent().getFileName().toString().equals("region")).findFirst();
			if (found.isEmpty()) {
				throw new AssertionError("No r.0.0.mca under " + worldDir);
			}
			return found.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private CompoundTag read(Path path) {
		try {
			return DataFileUtil.read(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void write(Path path, String snbt) {
		try {
			DataFileUtil.write(path, SnbtUtil.parseCompound(snbt, SnbtDialect.MODERN));
		} catch (Exception e) {
			throw new AssertionError("Couldn't write " + path, e);
		}
	}

	private void writeImage(Path path) {
		BufferedImage image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 8; y++) {
				image.setRGB(x, y, 0xFF000000 | (255 - y * 30) << 16 | (x * 12) << 8 | (y * 30));
			}
		}
		try {
			Files.createDirectories(path.getParent());
			ImageIO.write(image, "png", path.toFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void drop(ClientGameTestContext context, Path path) {
		context.runOnClient(client -> {
			UiScreen ui = this.ui(client);
			if (ui == null || !ui.filesDropped(List.of(path))) {
				throw new AssertionError("The screen didn't take the dropped file " + path);
			}
		});
		context.waitTicks(20);
	}

	private void clickChunk(ClientGameTestContext context, int index) {
		double[] center = context.computeOnClient(client -> {
			Widget grid = this.find(this.ui(client), "RegionGrid");
			if (grid == null) {
				throw new AssertionError("No region grid on screen");
			}
			int cell = Math.max(2, Math.min(grid.width(), grid.height()) / REGION_SIDE);
			int originX = grid.x() + (grid.width() - cell * REGION_SIDE) / 2;
			int originY = grid.y() + (grid.height() - cell * REGION_SIDE) / 2;
			double scale = client.getWindow().getGuiScale();
			return new double[] {(originX + (index & 31) * cell + cell / 2.0) * scale, (originY + (index >> 5) * cell + cell / 2.0) * scale};
		});
		context.getInput().setCursorPos(center[0], center[1]);
		context.waitTicks(3);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(20);
	}

	private Widget find(WidgetGroup group, String name) {
		if (group == null) {
			return null;
		}
		for (Widget child : group.children()) {
			if (child.getClass().getSimpleName().equals(name)) {
				return child;
			}
			if (child instanceof WidgetGroup) {
				Widget found = this.find((WidgetGroup) child, name);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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
