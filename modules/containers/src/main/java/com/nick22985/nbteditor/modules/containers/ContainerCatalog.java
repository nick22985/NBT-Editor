package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.container.ContainerSlotDef;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.DyeColor;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.platform.EntityTrait;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.ui.SlotIcon;
import com.nick22985.nbteditor.util.PlayerInventoryUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerCatalog {
	public static final String ENDER_CHEST = "ender_chest";
	private static final String MC = "minecraft:";
	private static final Set<EquipmentSlot> MOB_SLOTS = EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

	private final Map<String, ContainerFormat> formats = new LinkedHashMap<>();
	private final EntityHost entityHost = new EntityHost();

	public ContainerCatalog() {
		this.blocks();
		this.entities();
		this.items();
		this.add(ENDER_CHEST, subject -> subject.kind() == SubjectKind.PLAYER, this.entityHost, new ItemsStore(PlayerInventoryUtil.ENDER_ITEMS, ContainerLayout.grid(9, 3), 0));
	}

	public Map<String, ContainerFormat> formats() {
		return this.formats;
	}

	private void blocks() {
		List<String> shulkers = new ArrayList<>(List.of("shulker_box"));
		for (DyeColor color : DyeColor.values()) {
			shulkers.add(color.id() + "_shulker_box");
		}
		this.block("chest", "chest", this.ids("chest"), ItemsStore.of(ContainerLayout.grid(9, 3)));
		this.block("trapped_chest", "trapped_chest", this.ids("trapped_chest"), ItemsStore.of(ContainerLayout.grid(9, 3)));
		this.block("barrel", "barrel", this.ids("barrel"), ItemsStore.of(ContainerLayout.grid(9, 3)));
		this.block("shulker_box", "shulker_box", this.ids(shulkers.toArray(new String[0])), ItemsStore.of(ContainerLayout.grid(9, 3)));
		this.add("copper_chest", this.since(this.blockSuffix("copper_chest"), GameVersion.V1_21_9), new BlockHost(MC + "chest"), ItemsStore.of(ContainerLayout.grid(9, 3)));
		ContainerLayout furnace = ContainerLayout.of(List.of(ContainerSlotDef.of(0, 0), ContainerSlotDef.of(0, 36), ContainerSlotDef.of(56, 18)));
		this.block("furnace", "furnace", this.ids("furnace"), ItemsStore.of(furnace));
		this.block("blast_furnace", "blast_furnace", this.ids("blast_furnace"), ItemsStore.of(furnace));
		this.block("smoker", "smoker", this.ids("smoker"), ItemsStore.of(furnace));
		ContainerLayout brewing = ContainerLayout.of(List.of(new ContainerSlotDef(39, 34, SlotIcon.POTION), new ContainerSlotDef(62, 41, SlotIcon.POTION), new ContainerSlotDef(85, 34, SlotIcon.POTION), ContainerSlotDef.of(62, 0), new ContainerSlotDef(0, 0, SlotIcon.BREWING_FUEL)));
		this.block("brewing_stand", "brewing_stand", this.ids("brewing_stand"), ItemsStore.of(brewing));
		this.block("campfire", "campfire", this.ids("campfire", "soul_campfire"), ItemsStore.of(ContainerLayout.grid(4, 1)));
		this.block("dispenser", "dispenser", this.ids("dispenser"), ItemsStore.of(ContainerLayout.grid(3, 3)));
		this.block("dropper", "dropper", this.ids("dropper"), ItemsStore.of(ContainerLayout.grid(3, 3)));
		this.block("crafter", "crafter", this.ids("crafter"), ItemsStore.of(ContainerLayout.grid(3, 3)));
		this.block("hopper", "hopper", this.ids("hopper"), ItemsStore.of(ContainerLayout.grid(5, 1)));
		this.block("jukebox", "jukebox", this.ids("jukebox"), KeyStore.of("RecordItem"));
		this.block("lectern", "lectern", this.ids("lectern"), KeyStore.of("Book"));
		this.block("chiseled_bookshelf", "chiseled_bookshelf", this.ids("chiseled_bookshelf"), new BookshelfStore());
		this.block("brushable_block", "brushable_block", this.ids("suspicious_sand", "suspicious_gravel"), KeyStore.of("item"));
		this.block("decorated_pot", "decorated_pot", this.ids("decorated_pot"), new PotStore());
		this.add("shelf", this.since(this.blockSuffix("_shelf"), GameVersion.V1_21_9), new BlockHost(MC + "shelf"), ItemsStore.of(ContainerLayout.grid(3, 1)));
	}

	private void entities() {
		this.entity("chest_minecart", this.entityIds("chest_minecart"), ItemsStore.of(ContainerLayout.grid(9, 3)));
		this.entity("hopper_minecart", this.entityIds("hopper_minecart"), ItemsStore.of(ContainerLayout.grid(5, 1)));
		this.add("chest_boat", subject -> this.entityKind(subject) && (subject.entity().endsWith("_chest_boat") || subject.entity().endsWith("_chest_raft") || subject.entity().equals(MC + "chest_boat")), this.entityHost, ItemsStore.of(ContainerLayout.grid(9, 3)));
		this.entity("armor_stand", this.entityIds("armor_stand"), this.equipment(MOB_SLOTS, null, SlotIcon.NONE));
		this.mob("horse", this.entityIds("horse"), subject -> List.of(this.equipment(this.slots(subject, EquipmentSlot.BODY, EquipmentSlot.SADDLE), "ArmorItem", SlotIcon.HORSE_ARMOR)));
		this.mob("saddled_horse", this.entityIds("skeleton_horse", "zombie_horse", "camel", "camel_husk"), subject -> List.of(this.equipment(this.slots(subject, EquipmentSlot.SADDLE), null, SlotIcon.NONE)));
		this.mob("chested_horse", this.entityIds("donkey", "mule"), subject -> List.of(this.equipment(this.slots(subject, EquipmentSlot.SADDLE), null, SlotIcon.NONE), new ChestStore(false)));
		this.mob("llama", this.entityIds("llama", "trader_llama"), subject -> List.of(this.equipment(this.slots(subject, EquipmentSlot.BODY), "DecorItem", SlotIcon.LLAMA_ARMOR), new ChestStore(true)));
		this.mob("saddled", this.entityIds("pig", "strider"), subject -> List.of(Feature.ENTITY_EQUIPMENT.in(subject.version()) ? this.equipment(this.slots(subject, EquipmentSlot.SADDLE), null, SlotIcon.NONE) : this.equipment(this.slots(subject), null, SlotIcon.NONE)));
		this.mob("wolf", this.entityIds("wolf"), subject -> List.of(Feature.BODY_ARMOR_ITEM.in(subject.version()) ? this.equipment(this.slots(subject, EquipmentSlot.BODY), null, SlotIcon.NONE) : this.equipment(this.slots(subject), null, SlotIcon.NONE)));
		this.mob("body", this.entityIds("happy_ghast"), subject -> List.of(this.equipment(this.slots(subject, EquipmentSlot.BODY), null, SlotIcon.NONE)));
		this.mob("villager", this.entityIds("villager", "wandering_trader", "piglin"), subject -> List.of(this.equipment(this.slots(subject), null, SlotIcon.NONE), new PlainListStore("Inventory", ContainerLayout.grid(4, 2))));
		this.mob("pillager", this.entityIds("pillager"), subject -> List.of(this.equipment(this.slots(subject), null, SlotIcon.NONE), new PlainListStore("Inventory", ContainerLayout.grid(5, 1))));
		this.mob("allay", this.entityIds("allay"), subject -> List.of(this.equipment(this.slots(subject), null, SlotIcon.NONE), new PlainListStore("Inventory", ContainerLayout.grid(1, 1))));
		this.entity("item_frame", this.entityIds("item_frame", "glow_item_frame", "item"), KeyStore.of("Item"));
		this.entity("item_display", this.entityIds("item_display", "ominous_item_spawner"), KeyStore.of("item"));
		this.mob("mob", subject -> this.entityKind(subject) && subject.has(EntityTrait.MOB), subject -> List.of(this.equipment(Feature.BODY_ARMOR_ITEM.in(subject.version()) ? this.slots(subject, EquipmentSlot.BODY) : MOB_SLOTS, null, SlotIcon.NONE)));
	}

	private void items() {
		this.add("bundle", subject -> subject.kind() == SubjectKind.ITEM && subject.id().endsWith("bundle"), this.entityHost, new BundleStore());
	}

	private Set<EquipmentSlot> slots(ContainerSubject subject, EquipmentSlot... extra) {
		Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);
		if (subject.has(EntityTrait.MOB) || subject.traits().isEmpty()) {
			slots.addAll(MOB_SLOTS);
		}
		slots.addAll(List.of(extra));
		return slots;
	}

	private EquipmentStore equipment(Set<EquipmentSlot> slots, String legacyBody, SlotIcon bodyIcon) {
		return new EquipmentStore(slots, legacyBody, bodyIcon);
	}

	private Set<String> ids(String... ids) {
		Set<String> out = new HashSet<>();
		for (String id : ids) {
			out.add(MC + id);
		}
		return out;
	}

	private Predicate<ContainerSubject> entityIds(String... ids) {
		Set<String> set = this.ids(ids);
		return subject -> this.entityKind(subject) && set.contains(subject.entity());
	}

	private boolean entityKind(ContainerSubject subject) {
		return subject.entity() != null && (subject.kind() == SubjectKind.ENTITY || subject.kind() == SubjectKind.ITEM);
	}

	private boolean blockKind(ContainerSubject subject) {
		return subject.kind() == SubjectKind.BLOCK || subject.kind() == SubjectKind.ITEM && subject.entity() == null;
	}

	private Predicate<ContainerSubject> blockSuffix(String suffix) {
		return subject -> this.blockKind(subject) && subject.id().endsWith(suffix) && !subject.id().endsWith("bookshelf");
	}

	private Predicate<ContainerSubject> since(Predicate<ContainerSubject> matcher, GameVersion version) {
		return subject -> subject.version().atLeast(version) && matcher.test(subject);
	}

	private void block(String id, String blockEntity, Set<String> blocks, SlotStore store) {
		this.add(id, subject -> this.blockKind(subject) && blocks.contains(subject.id()), new BlockHost(MC + blockEntity), store);
	}

	private void entity(String id, Predicate<ContainerSubject> matcher, SlotStore store) {
		this.add(id, matcher, this.entityHost, store);
	}

	private void mob(String id, Predicate<ContainerSubject> matcher, Function<ContainerSubject, List<SlotStore>> stores) {
		this.formats.put(id, new SpecFormat(matcher, this.entityHost, stores));
	}

	private void add(String id, Predicate<ContainerSubject> matcher, ContainerHost host, SlotStore store) {
		this.formats.put(id, SpecFormat.of(matcher, host, store));
	}
}
