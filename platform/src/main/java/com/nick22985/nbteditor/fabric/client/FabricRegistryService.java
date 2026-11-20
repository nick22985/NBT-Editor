package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.util.McBlockUtil;
import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.util.McEnchantmentUtil;
import com.nick22985.nbteditor.fabric.util.McEntityUtil;
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.fabric.util.McRegistryUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.platform.EnchantmentInfo;
import com.nick22985.nbteditor.platform.EntityTrait;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricRegistryService implements RegistryService {
	private final Map<String, Set<EntityTrait>> traits = new HashMap<>();
	private Level traitLevel;

	@Override
	public List<String> ids(String registry) {
		if (DIMENSION.equals(registry)) {
			return this.dimensions();
		}
		Optional<Registry<Object>> found = McRegistryUtil.registry(NbteRuntime.INSTANCE.registries().get(), registry);
		if (found.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Identifier key : found.get().keySet()) {
			out.add(key.toString());
		}
		Collections.sort(out);
		return out;
	}

	private List<String> dimensions() {
		Set<String> out = new LinkedHashSet<>();
		out.add("minecraft:overworld");
		out.add("minecraft:the_nether");
		out.add("minecraft:the_end");
		MinecraftServer server = NbteRuntime.INSTANCE.registries().server();
		if (server != null) {
			for (ResourceKey<Level> key : server.levelKeys()) {
				out.add(key.identifier().toString());
			}
		} else if (Minecraft.getInstance().getConnection() != null) {
			for (ResourceKey<Level> key : Minecraft.getInstance().getConnection().levels()) {
				out.add(key.identifier().toString());
			}
		}
		return new ArrayList<>(out);
	}

	@Override
	public boolean contains(String registry, String id) {
		if (DIMENSION.equals(registry)) {
			return this.dimensions().contains(id);
		}
		Identifier key = McIdUtil.tryParse(id);
		if (key == null) {
			return false;
		}
		Optional<Registry<Object>> found = McRegistryUtil.registry(NbteRuntime.INSTANCE.registries().get(), registry);
		return found.isPresent() && found.get().containsKey(key);
	}

	@Override
	public Text name(String registry, String id) {
		Identifier key = McIdUtil.tryParse(id);
		if (key == null) {
			return Text.literal(id);
		}
		if (ITEM.equals(registry) && BuiltInRegistries.ITEM.containsKey(key)) {
			return McTextUtil.fromMc(new ItemStack(BuiltInRegistries.ITEM.getValue(key)).getHoverName());
		}
		if (BLOCK.equals(registry) && BuiltInRegistries.BLOCK.containsKey(key)) {
			return McTextUtil.fromMc(BuiltInRegistries.BLOCK.getValue(key).getName());
		}
		if (ENTITY_TYPE.equals(registry) && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
			return McTextUtil.fromMc(type.getDescription());
		}
		String translation = this.translationKey(registry, key);
		if (translation != null && Language.getInstance().has(translation)) {
			return Text.translatable(translation);
		}
		return Text.literal(key.getNamespace().equals("minecraft") ? key.getPath() : key.toString());
	}

	private String translationKey(String registry, Identifier key) {
		return switch (registry) {
			case ENCHANTMENT -> "enchantment." + key.getNamespace() + "." + key.getPath();
			case MOB_EFFECT -> "effect." + key.getNamespace() + "." + key.getPath();
			case POTION -> "item.minecraft.potion.effect." + key.getPath();
			case ATTRIBUTE -> "attribute.name." + key.getPath();
			case TRIM_MATERIAL -> "trim_material." + key.getNamespace() + "." + key.getPath();
			case TRIM_PATTERN -> "trim_pattern." + key.getNamespace() + "." + key.getPath();
			default -> null;
		};
	}

	@Override
	public Map<String, List<String>> blockProperties(String blockId) {
		Block block = McBlockUtil.block(blockId);
		return block == null ? Map.of() : McBlockUtil.propertyValues(block);
	}

	@Override
	public Map<String, String> defaultBlockState(String blockId) {
		Block block = McBlockUtil.block(blockId);
		return block == null ? Map.of() : McBlockUtil.properties(block.defaultBlockState());
	}

	@Override
	public String blockItem(String blockId) {
		Block block = McBlockUtil.block(blockId);
		if (block == null || block.asItem() == Items.AIR) {
			return null;
		}
		return BuiltInRegistries.ITEM.getKey(block.asItem()).toString();
	}

	@Override
	public EnchantmentInfo enchantment(String id) {
		Enchantment enchantment = McEnchantmentUtil.enchantment(id);
		return enchantment == null ? null : new EnchantmentInfo(id, enchantment.getMaxLevel(), McEnchantmentUtil.curse(enchantment));
	}

	@Override
	public boolean canEnchant(String enchantment, GameItem item) {
		Enchantment found = McEnchantmentUtil.enchantment(enchantment);
		return found != null && found.canEnchant(FabricGameItem.stackOf(item));
	}

	@Override
	public Set<EntityTrait> entityTraits(String entityId) {
		Level level = Minecraft.getInstance().level;
		if (level == null || entityId == null) {
			return Set.of();
		}
		if (level != this.traitLevel) {
			this.traits.clear();
			this.traitLevel = level;
		}
		return this.traits.computeIfAbsent(entityId, id -> this.probe(id, level));
	}

	private Set<EntityTrait> probe(String id, Level level) {
		EntityType<?> type = McEntityUtil.type(id);
		if (type == null) {
			return Set.of();
		}
		Entity entity;
		try {
			entity = McEntityUtil.create(type, level);
		} catch (RuntimeException e) {
			return Set.of();
		}
		Set<EntityTrait> out = EnumSet.noneOf(EntityTrait.class);
		if (entity instanceof LivingEntity) {
			out.add(EntityTrait.LIVING);
		}
		if (entity instanceof Mob) {
			out.add(EntityTrait.MOB);
		}
		return Set.copyOf(out);
	}
}
