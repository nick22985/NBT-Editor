package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.platform.EntityTrait;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class ContainerCatalogTest {
	private static final Set<EntityTrait> MOB = EnumSet.of(EntityTrait.LIVING, EntityTrait.MOB);

	private final ContainerCatalog catalog = new ContainerCatalog();

	private ContainerSubject subject(SubjectKind kind, String id, String entity, Set<EntityTrait> traits, String snbt, GameVersion version) throws Exception {
		return new ContainerSubject(kind, id, entity, traits, SnbtUtil.parseCompound(snbt, SnbtDialect.MODERN), version);
	}

	private Optional<ContainerFormat> format(ContainerSubject subject) {
		for (ContainerFormat format : this.catalog.formats().values()) {
			if (format.supports(subject)) {
				return Optional.of(format);
			}
		}
		return Optional.empty();
	}

	private CompoundTag item(String id, int count, GameVersion version) {
		return ItemStackUtil.create(id, count, version);
	}

	private List<CompoundTag> roundTrip(ContainerFormat format, ContainerSubject subject, List<CompoundTag> items) {
		format.write(subject, items);
		return format.read(subject);
	}

	@Test
	void chestBlock() throws Exception {
		ContainerSubject subject = this.subject(SubjectKind.BLOCK, "minecraft:chest", null, Set.of(), "{Name:\"minecraft:chest\",nbt:{id:\"minecraft:chest\",Items:[{Slot:3b,id:\"minecraft:stone\",count:5}]}}", GameVersion.V1_21);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals(27, items.size());
		assertEquals("minecraft:stone", ItemStackUtil.id(items.get(3)));
		assertTrue(items.get(0).isEmpty());
		items.set(0, this.item("minecraft:dirt", 2, GameVersion.V1_21));
		List<CompoundTag> after = this.roundTrip(format, subject, items);
		assertEquals("minecraft:dirt", ItemStackUtil.id(after.get(0)));
		assertEquals("minecraft:stone", ItemStackUtil.id(after.get(3)));
		assertEquals(2, subject.root().getCompound("nbt").getList("Items").size());
	}

	@Test
	void shulkerItemComponents() throws Exception {
		ContainerSubject subject = this.subject(SubjectKind.ITEM, "minecraft:red_shulker_box", null, Set.of(), "{id:\"minecraft:red_shulker_box\",count:1,components:{\"minecraft:container\":[{slot:2,item:{id:\"minecraft:dirt\",count:1}}]}}", GameVersion.V1_21);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals(27, items.size());
		assertEquals("minecraft:dirt", ItemStackUtil.id(items.get(2)));
		items.set(2, new CompoundTag());
		items.set(26, this.item("minecraft:stone", 1, GameVersion.V1_21));
		List<CompoundTag> after = this.roundTrip(format, subject, items);
		assertTrue(after.get(2).isEmpty());
		assertEquals("minecraft:stone", ItemStackUtil.id(after.get(26)));
		assertFalse(subject.root().getCompound("components").contains("minecraft:block_entity_data"));
	}

	@Test
	void shulkerItemLegacy() throws Exception {
		GameVersion version = GameVersion.V1_20_1;
		ContainerSubject subject = this.subject(SubjectKind.ITEM, "minecraft:shulker_box", null, Set.of(), "{id:\"minecraft:shulker_box\",Count:1b,tag:{BlockEntityTag:{Items:[{Slot:0b,id:\"minecraft:dirt\",Count:1b}]}}}", version);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals("minecraft:dirt", ItemStackUtil.id(items.get(0)));
		List<CompoundTag> cleared = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			cleared.add(new CompoundTag());
		}
		format.write(subject, cleared);
		assertFalse(subject.root().contains("tag"));
	}

	@Test
	void donkeyLegacyChest() throws Exception {
		GameVersion version = GameVersion.V1_20_1;
		ContainerSubject subject = this.subject(SubjectKind.ENTITY, "minecraft:donkey", "minecraft:donkey", MOB, "{id:\"minecraft:donkey\",Items:[{Slot:2b,id:\"minecraft:dirt\",Count:1b}]}", version);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals(22, items.size());
		assertEquals("minecraft:dirt", ItemStackUtil.id(items.get(7)));
		items.set(6, this.item("minecraft:saddle", 1, version));
		List<CompoundTag> after = this.roundTrip(format, subject, items);
		assertEquals("minecraft:saddle", ItemStackUtil.id(after.get(6)));
		assertEquals("minecraft:saddle", ItemStackUtil.id(subject.root().getCompound("SaddleItem")));
		assertTrue(subject.root().contains("ChestedHorse"));
	}

	@Test
	void mobEquipmentModern() throws Exception {
		GameVersion version = GameVersion.V1_21_5;
		ContainerSubject subject = this.subject(SubjectKind.ENTITY, "minecraft:zombie", "minecraft:zombie", MOB, "{id:\"minecraft:zombie\",equipment:{head:{id:\"minecraft:diamond_helmet\",count:1}}}", version);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals(7, items.size());
		assertEquals("minecraft:diamond_helmet", ItemStackUtil.id(items.get(0)));
		items.set(0, new CompoundTag());
		this.roundTrip(format, subject, items);
		assertFalse(subject.root().contains("equipment"));
	}

	@Test
	void mobBodyArmorItem() throws Exception {
		GameVersion version = GameVersion.V1_20_5;
		ContainerSubject subject = this.subject(SubjectKind.ENTITY, "minecraft:zombie", "minecraft:zombie", MOB, "{id:\"minecraft:zombie\",body_armor_item:{id:\"minecraft:diamond_horse_armor\",count:1}}", version);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals(7, items.size());
		assertEquals("minecraft:diamond_horse_armor", ItemStackUtil.id(items.get(6)));
		items.set(6, this.item("minecraft:iron_horse_armor", 1, version));
		this.roundTrip(format, subject, items);
		assertEquals("minecraft:iron_horse_armor", ItemStackUtil.id(subject.root().getCompound("body_armor_item")));
	}

	@Test
	void mobEquipmentLegacy() throws Exception {
		GameVersion version = GameVersion.V1_20_1;
		ContainerSubject subject = this.subject(SubjectKind.ENTITY, "minecraft:zombie", "minecraft:zombie", MOB, "{id:\"minecraft:zombie\",ArmorItems:[{},{},{},{id:\"minecraft:diamond_helmet\",Count:1b}],HandItems:[{},{}]}", version);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals("minecraft:diamond_helmet", ItemStackUtil.id(items.get(0)));
		items.set(4, this.item("minecraft:iron_sword", 1, version));
		this.roundTrip(format, subject, items);
		assertEquals("minecraft:iron_sword", ItemStackUtil.id(subject.root().getList("HandItems").getCompound(0)));
	}

	@Test
	void bundleCompacts() throws Exception {
		GameVersion version = GameVersion.V1_21;
		ContainerSubject subject = this.subject(SubjectKind.ITEM, "minecraft:bundle", null, Set.of(), "{id:\"minecraft:bundle\",count:1,components:{\"minecraft:bundle_contents\":[{id:\"minecraft:dirt\",count:1}]}}", version);
		ContainerFormat format = this.format(subject).orElseThrow();
		List<CompoundTag> items = format.read(subject);
		assertEquals("minecraft:dirt", ItemStackUtil.id(items.get(0)));
		items.set(5, this.item("minecraft:stone", 1, version));
		List<CompoundTag> after = this.roundTrip(format, subject, items);
		assertEquals("minecraft:stone", ItemStackUtil.id(after.get(1)));
	}

	@Test
	void plainBlockUnsupported() throws Exception {
		ContainerSubject subject = this.subject(SubjectKind.BLOCK, "minecraft:stone", null, Set.of(), "{Name:\"minecraft:stone\"}", GameVersion.V1_21);
		assertTrue(this.format(subject).isEmpty());
	}

	@Test
	void copperChestNeedsVersion() throws Exception {
		String snbt = "{Name:\"minecraft:copper_chest\",nbt:{id:\"minecraft:chest\"}}";
		assertTrue(this.format(this.subject(SubjectKind.BLOCK, "minecraft:copper_chest", null, Set.of(), snbt, GameVersion.V1_21_9)).isPresent());
		assertTrue(this.format(this.subject(SubjectKind.BLOCK, "minecraft:copper_chest", null, Set.of(), snbt, GameVersion.V1_21_5)).isEmpty());
	}
}
