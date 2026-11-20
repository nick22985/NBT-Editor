package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.platform.ResourceService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
//? if >=1.19 {
import java.util.Optional;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricResourceService implements ResourceService {
	@Override
	public String read(String id) {
		Identifier location = McIdUtil.tryParse(id);
		if (location == null) {
			return null;
		}
		ResourceManager manager = Minecraft.getInstance().getResourceManager();
		//? if >=1.19 {
		Optional<Resource> resource = manager.getResource(location);
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream in = resource.get().open()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
		//?} else {
		/*try (Resource resource = manager.getResource(location); InputStream in = resource.getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			return null;
		}
		*///?}
	}

	@Override
	public List<String> list(String folder) {
		ResourceManager manager = Minecraft.getInstance().getResourceManager();
		List<String> out = new ArrayList<>();
		//? if >=1.19 {
		for (Identifier location : manager.listResources(folder, path -> path.getPath().endsWith(".json")).keySet()) {
			out.add(location.toString());
		}
		//?} else {
		/*for (Identifier location : manager.listResources(folder, path -> path.endsWith(".json"))) {
			out.add(location.toString());
		}
		*///?}
		return out;
	}

	@Override
	public String pack(String id) {
		Identifier location = McIdUtil.tryParse(id);
		if (location == null) {
			return null;
		}
		Minecraft mc = Minecraft.getInstance();
		String source;
		//? if >=1.19 {
		Optional<Resource> resource = mc.getResourceManager().getResource(location);
		if (resource.isEmpty()) {
			return null;
		}
		source = resource.get().sourcePackId();
		//?} else {
		/*try (Resource resource = mc.getResourceManager().getResource(location)) {
			source = resource.getSourceName();
		} catch (IOException | RuntimeException e) {
			return null;
		}
		*///?}
		Pack pack = mc.getResourcePackRepository().getPack(source);
		return pack == null ? source : pack.getTitle().getString();
	}

	@Override
	public List<String> packs() {
		List<String> out = new ArrayList<>();
		for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
			out.add(0, pack.getTitle().getString());
		}
		return out;
	}
}
