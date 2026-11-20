package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record AttributeModifier(String attribute, double amount, AttributeOperation operation, String slot, String id, CompoundTag extra) {
	public static final String ANY = "any";

	public static AttributeModifier create(String attribute, double amount, AttributeOperation operation, String slot, GameVersion version) {
		String id = version.atLeast(GameVersion.V1_21) ? "minecraft:" + UUID.randomUUID() : UUID.randomUUID().toString();
		return new AttributeModifier(attribute, amount, operation, slot, id, new CompoundTag());
	}

	public AttributeModifier withAttribute(String attribute) {
		return new AttributeModifier(attribute, amount, operation, slot, id, extra);
	}

	public AttributeModifier withAmount(double amount) {
		return new AttributeModifier(attribute, amount, operation, slot, id, extra);
	}

	public AttributeModifier withOperation(AttributeOperation operation) {
		return new AttributeModifier(attribute, amount, operation, slot, id, extra);
	}

	public AttributeModifier withSlot(String slot) {
		return new AttributeModifier(attribute, amount, operation, slot, id, extra);
	}

	public AttributeModifier withId(String id) {
		return new AttributeModifier(attribute, amount, operation, slot, id, extra);
	}
}
